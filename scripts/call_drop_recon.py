import MySQLdb
import json
import requests

HOST = '127.0.0.1'
USER = 'root'
PASSWORD = ''
DATABASE = 'statesman_db_'
SHARDS = 16

START_STATE_WORKFLOW_SQL = """ select workflow_id from workflow_instances where current_state = 'START' and updated < DATE_SUB(NOW(), INTERVAL 1 HOUR) """

STATESMAN_RECON_URL = "https://127.0.0.1/v1/housekeeping/trigger/workflow/{}"
HEADERS = {
    "Content-Type": "application/json"
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
        print("INFO COMPLETED SHARD: " + str(i))
        mainDb.commit()
        cursor.close()
        mainDb.close()
    return result


#######################  WORKFLOW HELPER ##########################

def mark_call_drop(workflow_id):
    try:
        print("Reconciling workflow:" + workflow_id)
        payload = {"callDropped": True}
        response = requests.post(url=STATESMAN_RECON_URL.format(workflow_id), data=json.dumps(payload), headers=HEADERS)
        if response.status_code == 200:
            return response.json()
        else:
            return {}
    except:
        pass
    return {}


def recon_start_state_workflow():
    result = execute_query(START_STATE_WORKFLOW_SQL)
    for row in result:
        mark_call_drop(row[0])


recon_start_state_workflow()