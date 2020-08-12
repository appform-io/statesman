#!/usr/bin/env python
from os import listdir
from os.path import isfile, join
import csv
import json
import persistqueue
import requests
import shutil
import time
import calendar
import math

scanpath='upload_covid_monitoring'
processedPath='processed_covid_monitoring'
rows = []
csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ('covid-monitoring')
statesmanUrl = "http://localhost:8080"
phones = set()
stateWorkflows = {"bihar":"77ee9073-eed9-4fbb-8150-31d96af4a536","maharashtra":"7735772e-523c-45f2-b64d-116489048a2e","delhi": "3efd0e4b-a6cc-4e59-9f88-bb0141a66142","punjab":"933bed6c-e6a6-4de4-9ea8-7a31d64a08dc','11dd4791-472b-454b-8f7a-39a589a6335c"}

def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000


def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))


def trigger_new_workflow(payload,mobileNumber,wfSource):
    for i in range(3):
        r = requests.post(statesmanUrl + '/callbacks/ingress/raw/'+wfSource, data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for mobileNumber: ' + mobileNumber)
            return True
        else:
            print(str(i) + ': could not post data for mobileNumber: ' + mobileNumber + ' status:' + str(r.status_code))
    return False


def existing_workflow(phone,state):
    finalFql = """ select eventData.workflowId from statesman where eventData.workflowTemplateId in ('%s') and eventType = 'STATE_CHANGED' and eventData.newState in ('HOME_QUARANTINE','HI_ONBOARD')  and eventData.data.mobile_number = '%s' limit 1  """ % (stateWorkflows[state], str(phone))
    #print(finalFql)
    r = requests.post('https://foxtrot.telemed-ind.appform.io/foxtrot/v1/fql', data=finalFql, headers = {"Accept": "application/json",'content-type': 'application/json','Authorization':'Bearer eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJmb3h0cm90LXNlcnZlciIsImp0aSI6IjhiMDk0MzkxLWNhYWYtNDg5MC04NTg1LWYyYWY1Y2MyOTUxMCIsImlhdCI6MTU4Njc4Mjg5NCwibmJmIjoxNTg2NzgyNzc0LCJzdWIiOiJyZXBvcnRpbmciLCJhdWQiOiJTVEFUSUMifQ.xdqRera5ZhNhzbxYtLmk2L05n_iqyfVRZiU9NGodR8iH5nQOwMmJUXUeIb92JHd2ehVHmNF9v1L50CH_txLmYw'})
    if(r.status_code == 200):
        for row in r.json()['rows']:
            return row['eventData.workflowId']
    return None


def update_workflow(w,payload,mobileNumber):
    for i in range(3):
        r = requests.get(statesmanUrl + '/v1/housekeeping/debug/workflow/'+w, headers = {'content-type': 'application/json'})
        if(r.status_code != 200):
            print(str(i) + ': could not get data for mobileNumber: ' + mobileNumber + ' workflowId '+ w +  ' status:' + str(r.status_code))
        else:
            workflow = r.json()
            wd = workflow['dataObject']['data']
            if(wd["endTime"] >= payload['body']["endTime"]):
                print("Nothing to update for mobileNumber:" + mobileNumber + ' workflowId:'+ w )
                return True
            else:
                wd["endTime"] = payload['body']["endTime"]
                wd["end_date"] = payload['body']["end_date"]
                wd["maxDays"] = day_diff(workflow['created'],wd["endTime"])
                if(workflow['dataObject']['currentState']['name'] == "END"):
                    workflow['dataObject']['currentState']['name'] = "HOME_QUARANTINE"
                    workflow['dataObject']['currentState']['terminal'] = False

                r = requests.put(statesmanUrl + '/v1/housekeeping/update/workflow',data = json.dumps(workflow) , headers = {'content-type': 'application/json'})
                if(r.status_code == 200):
                    print("Updated for mobileNumber:" + mobileNumber + ' workflowId:'+ w  )
                    return True
                else:
                    print(str(i) + ': could not update data for mobileNumber: ' + mobileNumber + ' workflowId:'+ w +  ' status:' + str(r.status_code))
    return False

for csvFileName in csvFileNames:
    fqFileName=scanpath + '/' + csvFileName
    fqDestFilename=processedPath + '/' + csvFileName
    try:
        with open(fqFileName) as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                try:
                    convrow = dict((k.lower().strip().replace(' ', '_'), v.strip().replace('\n', "").replace('\r',"")) for k,v in row.iteritems())
                    if(convrow.has_key("")):
                        del convrow[""]
                    if(convrow.has_key("district")):
                        convrow["district"] = convrow['district'].lower().strip()
                    convrow['state'] = convrow['state'].lower().strip()
                    flow = convrow['flow'].lower().strip()
                    if(not stateWorkflows.has_key(convrow['state'])):
                        print("Error: Inavlid state mentioned skiping row:" + str(row))
                        continue
                    if(convrow['mobile_number'] in phones):
                        print("INFO: Already processed the mobile_number:" + convrow['mobile_number'])
                    phones.add(convrow['mobile_number'])
                    convrow['wfSource'] = convrow['state'] + '_'+ flow +'_monitoring_csv'
                    endTime = epoch_time(convrow['end_date'])
                    convrow['maxDays'] = day_diff(now(),endTime)
                    convrow['endTime'] = endTime
                    body = { 'id' : convrow['wfSource'] , 'body' : convrow, 'apiPath' : csvFileName }
                    print('Queuing job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                    jobQueue.put(json.dumps(body))
                    print('Queued job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                except Exception as e:
                    print('Error processing row: ' + str(row))
                    print(e)
    except:
        print('Error processing file: ' + fqFileName)
    print('Items queued for file [' + csvFileName + ']: ' + str(jobQueue.size))
    shutil.move(fqFileName, fqDestFilename)
    print('Moved file ' + fqFileName + ' to ' + fqDestFilename)
print('Total queue size: ' + str(jobQueue.size))

while jobQueue.size > 0:
    payload = jobQueue.get()
    payloadDict = json.loads(payload)
    mobileNumber = str(payloadDict['body']['mobile_number'])
    state = str(payloadDict['body']['state'])
    wfSource = str(payloadDict['body']['wfSource'])
    w = existing_workflow(mobileNumber,state)
    if(w is None):
        if(trigger_new_workflow(payload,mobileNumber,wfSource)):
            jobQueue.ack(payload)
        else:
            jobQueue.ack_failed(payload)
    else:
        print("Has workflow to update for mobile_number:"+ mobileNumber)
        if(update_workflow(w, payloadDict, mobileNumber)):
            jobQueue.ack(payload)
        else:
            jobQueue.ack_failed(payload)

shutil.rmtree('covid-monitoring')
print('Processing complete')