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
STATESMAN_RECON_URL = "http://statesman.telemed-ind.appform.io:8080/v1/housekeeping/trigger/workflow/{}"
HEADERS = {
    "Content-Type": "application/json"
}
CURRENT_DATE = datetime.date.today()
DAY_START_TIME = (int(datetime.datetime(CURRENT_DATE.year, CURRENT_DATE.month, CURRENT_DATE.day, 0, 0, 0).strftime('%s'))) * 1000
CALLBACK_TEMPLATE = {
    "CALL_NEEDED_SPAM_CHECK_RETRY_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('CALL_NEEDED_SPAM_CHECK_RETRY_3') and updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE)",
        "callback_payload": {"notReachable": True}
    },
    "CALL_NEEDED_SPAM_CHECK_RETRY_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('CALL_NEEDED_SPAM_CHECK_RETRY_2') and updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE)",
        "callback_payload": {"retryAttempt3": True}
    },
    "CALL_NEEDED_SPAM_CHECK": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('CALL_NEEDED_SPAM_CHECK') and updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE)",
        "callback_payload": {"retryAttempt2": True}
    },
    "HOME_QUARANTINE": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HOME_QUARANTINE')",
        "callback_payload": {"callTrigger": True, "now": DAY_START_TIME}
    },
    "IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('IVR_ATTEMPT_1')",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('IVR_ATTEMPT_2')",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('IVR_ATTEMPT_3')",
        "callback_payload": {"status": "noanswer"}
    },
    "HQ_VOILATION": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HQ_VOILATION')",
        "callback_payload": {"dayEnd": True}
    },
    "HI_ONBOARD": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_ONBOARD')",
        "callback_payload": {"onboard": True}
    },
    "HOME_ISOLATION": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HOME_ISOLATION')",
        "callback_payload": {"now": DAY_START_TIME}
    },
    "HI_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_IVR_START')",
        "callback_payload": {"callTrigger": True}
    },
    "HI_PUNJAB_ONBOARD_DOCTOR_FOLLOW": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_ONBOARD_DOCTOR_FOLLOW') and template_id in ('11dd4791-472b-454b-8f7a-39a589a6335c')",
        "callback_payload": {"callTrigger": True}
    },
    "HI_DAILY_FOLLOWUP": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_DAILY_FOLLOWUP')",
        "callback_payload": {"callTrigger": True}
    },
    "HI_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_IVR_ATTEMPT_1')",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_IVR_ATTEMPT_2')",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_IVR_ATTEMPT_3')",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_PUNE_ONBOARD_DOCTOR_FOLLOW": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_ONBOARD_DOCTOR_FOLLOW') and template_id = '7735772e-523c-45f2-b64d-116489048a2e' ",
        "callback_payload": {"dayEnd": True}
    },
    "HI_PUNE_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PUNE_IVR_START') and template_id = '7735772e-523c-45f2-b64d-116489048a2e' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_PUNE_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PUNE_IVR_ATTEMPT_1') and template_id = '7735772e-523c-45f2-b64d-116489048a2e' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_PUNE_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PUNE_IVR_ATTEMPT_2') and template_id = '7735772e-523c-45f2-b64d-116489048a2e' ",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_PUNE_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PUNE_IVR_ATTEMPT_3') and template_id = '7735772e-523c-45f2-b64d-116489048a2e' ",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_BIHAR_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_BIHAR_IVR_START') and template_id = '77ee9073-eed9-4fbb-8150-31d96af4a536' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_BIHAR_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_BIHAR_IVR_ATTEMPT_1') and template_id = '77ee9073-eed9-4fbb-8150-31d96af4a536' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_BIHAR_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_BIHAR_IVR_ATTEMPT_2') and template_id = '77ee9073-eed9-4fbb-8150-31d96af4a536' ",
        "callback_payload": {"retryCallAttempt3": True}},
    "HI_BIHAR_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_BIHAR_IVR_ATTEMPT_3') and template_id = '77ee9073-eed9-4fbb-8150-31d96af4a536' ",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_TN_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_TN_IVR_START') and template_id = 'b2c71733-3da3-4e29-86ee-021565ba87b4' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_TN_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_TN_IVR_ATTEMPT_1') and template_id = 'b2c71733-3da3-4e29-86ee-021565ba87b4' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_TN_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_TN_IVR_ATTEMPT_2') and template_id = 'b2c71733-3da3-4e29-86ee-021565ba87b4' ",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_TN_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_TN_IVR_ATTEMPT_3') and template_id = 'b2c71733-3da3-4e29-86ee-021565ba87b4' ",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_PONDY_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PONDY_IVR_START') and template_id = 'fd20aa74-3210-4761-9e3b-4d6ab43477fb' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_PONDY_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PONDY_IVR_ATTEMPT_1') and template_id = 'fd20aa74-3210-4761-9e3b-4d6ab43477fb' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_PONDY_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PONDY_IVR_ATTEMPT_2') and template_id = 'fd20aa74-3210-4761-9e3b-4d6ab43477fb' ",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_PONDY_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_PONDY_IVR_ATTEMPT_3') and template_id = 'fd20aa74-3210-4761-9e3b-4d6ab43477fb' ",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_PONDY_ONBOARD_DOCTOR_FOLLOW": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_ONBOARD_DOCTOR_FOLLOW') and template_id = 'fd20aa74-3210-4761-9e3b-4d6ab43477fb' ",
        "callback_payload": {"dayEnd": True}
    },
    "HI_UK_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_UK_IVR_START') and template_id = 'c606cfb1-3a43-419b-a6f9-d745270dea80' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_UK_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_UK_IVR_ATTEMPT_1') and template_id = 'c606cfb1-3a43-419b-a6f9-d745270dea80' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_UK_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_UK_IVR_ATTEMPT_2') and template_id = 'c606cfb1-3a43-419b-a6f9-d745270dea80' ",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_UK_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_UK_IVR_ATTEMPT_3') and template_id = 'c606cfb1-3a43-419b-a6f9-d745270dea80' ",
        "callback_payload": {"status": "noanswer"}
    },
    "HI_VOILATION": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_VOILATION')",
        "callback_payload": {"dayEnd": True}
    },
    "PLASMA_DONATION_INIT": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('PLASMA_DONATION_INIT')",
        "callback_payload": {"retargetDormant": True}
    },
    "VF_AP_INIT": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('VF_AP_INIT') and template_id = '29d5e34a-8a97-4a63-aaeb-f7b72857ca25' ",
        "callback_payload": {"onboardInit": True}
    },
    "VF_AP_ONBOARDED": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('VF_AP_ONBOARDED') and template_id = '29d5e34a-8a97-4a63-aaeb-f7b72857ca25' ",
        "callback_payload": {"now": DAY_START_TIME}
    },
    "VF_AP_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('VF_AP_IVR_START') and template_id = '29d5e34a-8a97-4a63-aaeb-f7b72857ca25' ",
        "callback_payload": {"callTrigger": True}
    },
    "VF_AP_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('VF_AP_IVR_ATTEMPT_1') and template_id = '29d5e34a-8a97-4a63-aaeb-f7b72857ca25' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "VF_AP_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('VF_AP_IVR_ATTEMPT_2') and template_id = '29d5e34a-8a97-4a63-aaeb-f7b72857ca25' ",
        "callback_payload": {"dayEnd": True}
    },
    "HI_KA_IVR_START": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_KA_IVR_START') and template_id = '6294a243-db91-41c1-85ca-f93c3e38d52f' ",
        "callback_payload": {"callTrigger": True}
    },
    "HI_KA_IVR_ATTEMPT_1": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_KA_IVR_ATTEMPT_1') and template_id = '6294a243-db91-41c1-85ca-f93c3e38d52f' ",
        "callback_payload": {"retryCallAttempt2": True}
    },
    "HI_KA_IVR_ATTEMPT_2": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_KA_IVR_ATTEMPT_2') and template_id = '6294a243-db91-41c1-85ca-f93c3e38d52f' ",
        "callback_payload": {"retryCallAttempt3": True}
    },
    "HI_KA_IVR_ATTEMPT_3": {
        "sql": "select workflow_id,current_state from workflow_instances where current_state IN ('HI_KA_IVR_ATTEMPT_3') and template_id = '6294a243-db91-41c1-85ca-f93c3e38d52f' ",
        "callback_payload": {"status": "noanswer"}
    }
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


def statesman_callback(template):
    sql = CALLBACK_TEMPLATE[template]['sql']
    print("INFO: SQL:" + sql)
    result = execute_query(sql)
    for row in result:
        try:
            jobData = dict()
            jobData['state'] = row[1]
            jobData['workflow_id'] = row[0]
            jobData['callback_payload'] = CALLBACK_TEMPLATE[template]['callback_payload']
            workQueue.put(jobData.copy())
        except:
            print("ERROR: Processing row:" + str(",".join(row)))
    print("INFO: SQL result pushed to queue")


def process(jobData):
    #print("INFO: workflow_id:" + jobData['workflow_id'] + " state:" + jobData['state'])
    callback(jobData['workflow_id'], jobData['callback_payload'])

parser = argparse.ArgumentParser()
parser.add_argument("-t",   "--template", action='append', required=True,  help="TemplateName for which callback needs to be triggered")
options = parser.parse_args()
templates = options.template

for template in templates:
    if(not CALLBACK_TEMPLATE.has_key(template)):
        print("ERROR: Invalid state:" + template)
        exit(1)

initThreads()
for template in templates:
    statesman_callback(template)

waitForCompleteion()
exit(0)