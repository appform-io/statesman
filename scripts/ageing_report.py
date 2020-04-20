import MySQLdb
import json
import requests
import time
import uuid

HOST = '127.0.0.1'
USER = 'root'
PASSWORD = ''
DATABASE = 'statesman_db_'
SHARDS = 16

AGEING_SQL = """ SELECT current_state as non_terminal_state, template_id as workflow_template_id,
					  (CASE WHEN updated > DATE_SUB(NOW(), INTERVAL 1 HOUR)  THEN  "LT_HR" 
						WHEN updated > DATE_SUB(NOW(), INTERVAL 2 HOUR)  and updated < DATE_SUB(NOW(), INTERVAL 1 HOUR)  THEN  "GT1_LT2_HR"
						WHEN updated > DATE_SUB(NOW(), INTERVAL 6 HOUR) and updated < DATE_SUB(NOW(), INTERVAL 2 HOUR)  THEN  "GT2_LT6_HR"
						WHEN updated > DATE_SUB(NOW(), INTERVAL 12 HOUR) and updated < DATE_SUB(NOW(), INTERVAL 6 HOUR)  THEN "GT6_LT12_HR"
						WHEN updated > DATE_SUB(NOW(), INTERVAL 24 HOUR) and updated < DATE_SUB(NOW(), INTERVAL 12 HOUR)  THEN "GT12_LT24_HR"
						WHEN updated < DATE_SUB(NOW(), INTERVAL 24 HOUR)  THEN "GT24_HR" END) as time_interval, count(1) as pending_workflows 
						FROM workflow_instances WHERE completed = false GROUP BY current_state,template_id,time_interval """

FOXTROT_URL = "https://127.0.0.1/foxtrot/v1/document/ageing"
HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Bearer "
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


#######################  FOXTROT HELPER ##########################


def add_document(payload):
    try:
        print(payload)
        response = requests.post(url=FOXTROT_URL, data=json.dumps(payload), headers=HEADERS)
        print(response.text)
        if response.status_code == 200:
            return response.json()
        else:
            return {}
    except:
        pass
    return {}


def create_document(current_state, template_id, time_interval):
    id = str(uuid.uuid1())
    current_time = int(time.time() * 1000)
    return {
        "id": id,
        "metadata": {
            "id": id,
            "rawStorageId": id,
            "time": current_time
        },
        "data": {
            "time": current_time,
            "eventType": "PENDING_WORKFLOW_EVENT",
            "eventSchemaVersion": "v1",
            "eventData": {
                "currentState": current_state,
                "templateId": template_id,
                "timeInterval": time_interval,
                "pendingWorkflows": 0
            }
        }
    }


def key(current_state, template_id, time_interval):
    return current_state + "_" + template_id + "_" + time_interval


def create_ageing_documents(result):
    documents = {}
    for row in result:
        current_state = str(row[0])
        template_id = str(row[1])
        time_interval = str(row[2])
        pending_workflows = int(row[3])
        data_key = key(current_state, template_id, time_interval)
        if (not documents.has_key(data_key)):
            documents[data_key] = create_document(current_state, template_id, time_interval)
        documents[data_key]["data"]["eventData"]["pendingWorkflows"] += pending_workflows
    return documents.values()


def add_ageing_documents():
    result = execute_query(AGEING_SQL)
    documents = create_ageing_documents(result)
    for document in documents:
        add_document(document)


add_ageing_documents()
