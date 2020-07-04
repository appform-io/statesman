import MySQLdb
import json
import requests
import argparse
from multiprocessing import Queue
import threading
import time
import datetime

HOST = '127.0.0.1'
USER = 'root'
PASSWORD = ''
DATABASE = 'statesman_db_'
SHARDS = 16
WORKING_THREADS = 8
EXIT_FLAG = 0
STATE_WORKFLOW_SQL = """ select workflow_id,current_state from workflow_instances where current_state IN ('%s') and updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE)"""
STATESMAN_RECON_URL = "http://statesman.telemed-ind.appform.io:8080/v1/housekeeping/trigger/workflow/{}"
HEADERS = {
    "Content-Type": "application/json"
}
CURRENT_DATE = datetime.date.today()
DAY_START_TIME = (int(datetime.datetime(CURRENT_DATE.year, CURRENT_DATE.month, CURRENT_DATE.day, 0, 0, 0).strftime('%s'))) * 1000
STATE_CALLBACK_PAYLOAD = {
    "CALL_NEEDED_SPAM_CHECK_RETRY_3" : {"notReachable": True},
    "CALL_NEEDED_SPAM_CHECK_RETRY_2": {"retryAttempt3": True},
    "CALL_NEEDED_SPAM_CHECK" : {"retryAttempt2": True},
    "HOME_QUARANTINE" : {"callTrigger": True, "now": DAY_START_TIME},
    "IVR_ATTEMPT_1" : {"retryCallAttempt2": True},
    "IVR_ATTEMPT_2" : {"retryCallAttempt3": True},
    "IVR_ATTEMPT_3" : {"status": "noanswer"},
    "HQ_VOILATION": {"dayEnd": True}
}

queueLock = threading.Lock()
workQueue = Queue()
threads = []

#######################  THREAD HELPER #########################

class ProcessingThread (threading.Thread):
    def __init__(self, threadId, name, q):
        threading.Thread.__init__(self)
        self.threadId = threadId
        self.name = name
        self.q = q

    def run(self):
        print("INFO: STARTING " + self.name)
        processDataByThread(self.q)
        print("INFO: EXISTING " + self.name)

def processDataByThread(q):
    global EXIT_FLAG
    global queueLock
    while not EXIT_FLAG:
        queueLock.acquire()
        if not q.empty():
            row = q.get()
            queueLock.release()
            process(row)
        else:
            queueLock.release()
            time.sleep(1)

def initThreads():
    for x in range(0,WORKING_THREADS):
        thread = ProcessingThread(x, "Thread-"+str(x), workQueue)
        thread.start()
        threads.append(thread)


def waitForCompleteion():
    global EXIT_FLAG
    # Wait for queue to empty
    try:
        while workQueue.qsize() > 0:
            print("INFO: SIZE :" + str(workQueue.qsize()))
            time.sleep(5)
            pass
    except KeyboardInterrupt:
        print("INFO: Ctrl-c PRESSED ...")

    # Notify threads it's time to exit
    EXIT_FLAG = 1

    # Wait for all threads to complete
    try:
        for thread in threads:
            thread.join()
        print("INFO: EXITING MAIN THREAD")
    except KeyboardInterrupt:
        print("INFO: Ctrl-c PRESSED ...")



#######################  MYSQL HELPER ##########################


def connection(database):
    db = MySQLdb.connect(HOST, USER, PASSWORD, database)
    return db


def execute_query(sql):
    result = []
    for i in range(SHARDS):
        mainDb = connection(DATABASE + str(i))  # getting connection
        cursor = mainDb.cursor(MySQLdb.cursors.SSCursor)  # fetch data in bactch not all at once
        # executing Query
        cursor.execute(sql)
        for row in cursor:
            result.append(row)
        print("INFO: COMPLETED SHARD:" + str(i))
        mainDb.commit()
        cursor.close()
        mainDb.close()
    return result


#######################  WORKFLOW HELPER ##########################

def callback(workflow_id, payload):
    try:
        str_payload = json.dumps(payload)
        print("INFO: Reconciling workflow:" + workflow_id + " payload:"+str_payload)
        response = requests.post(url=STATESMAN_RECON_URL.format(workflow_id), data=str_payload, headers=HEADERS)
        print(response.text)
        if response.status_code == 200:
            return response.json()
        else:
            return {}
    except:
        pass
    return {}


def statesman_callback(states):
    sql = STATE_WORKFLOW_SQL % ("','".join(states))
    print("INFO: SQL:" + sql)
    result = execute_query(sql)
    for row in result:
        try:
            jobData = dict()
            jobData['state'] = row[1]
            jobData['workflow_id'] = row[0]
            workQueue.put(jobData.copy())
        except:
            print("ERROR: Processing row:" + str(",".join(row)))
    print("INFO: SQL result pushed to queue")


def process(jobData):
    #print("INFO: workflow_id:" + jobData['workflow_id'] + " state:" + jobData['state'])
    callback(jobData['workflow_id'], STATE_CALLBACK_PAYLOAD[jobData['state']])

parser = argparse.ArgumentParser()
parser.add_argument("-s",   "--state", action='append', required=True,  help="State for which callback needs to be triggered")
options = parser.parse_args()
states = options.state

for state in states:
    if(not STATE_CALLBACK_PAYLOAD.has_key(state)):
        print("ERROR: Invalid state:" + state)
        exit(1)

initThreads()
statesman_callback(states)
waitForCompleteion()
exit(0)