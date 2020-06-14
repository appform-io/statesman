import MySQLdb
import json
import requests
import argparse

HOST = '127.0.0.1'
USER = 'root'
PASSWORD = ''
DATABASE = 'statesman_db_'
SHARDS = 16

STATE_WORKFLOW_SQL = """ select workflow_id,current_state from workflow_instances where current_state IN ('%s') and updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE) """

STATESMAN_RECON_URL = "http://statesman.telemed-ind.appform.io:8080/v1/housekeeping/trigger/workflow/{}"
HEADERS = {
    "Content-Type": "application/json"
}

STATE_CALLBACK_PAYLOAD = {
    "CALL_NEEDED_SPAM_CHECK_RETRY_2": {"retryAttempt3": True},
    "CALL_NEEDED_SPAM_CHECK" : {"retryAttempt2": True},
    "HOME_QUARANTINE" : {"callTrigger": True},
    "IVR_ATTEMPT_1" : {"retryCallAttempt2": True},
    "IVR_ATTEMPT_2" : {"retryCallAttempt3": True}
}

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
            state = row[1]
            workflow_id = row[0]
            callback(workflow_id,STATE_CALLBACK_PAYLOAD[state])
        except:
            print("ERROR: Processing row:" + str(",".join(row)))



parser = argparse.ArgumentParser()
parser.add_argument("-s",   "--state", action='append', required=True,  help="State for which callback needs to be triggered")
options = parser.parse_args()
states = options.state

for state in states:
    if(not STATE_CALLBACK_PAYLOAD.has_key(state)):
        print("ERROR: Invalid state:" + state)
        exit(1)

statesman_callback(states)