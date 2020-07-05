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

def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000


def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))


def trigger_new_workflow(payload,mobileNumber):
    for i in range(3):
        r = requests.post(statesmanUrl + '/callbacks/ingress/raw/delhi_hq_monitoring_csv', data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for mobileNumber: ' + mobileNumber)
            return True
        else:
            print(str(i) + ': could not post data for mobileNumber: ' + mobileNumber + ' status:' + str(r.status_code))
    return False


def existing_workflow(phone):
    finalFql = """ select eventData.workflowId from statesman where eventData.workflowTemplateId = '3efd0e4b-a6cc-4e59-9f88-bb0141a66142' and eventType = 'STATE_CHANGED' and eventData.newState = 'HOME_QUARANTINE' and eventData.data.mobile_number = '%s' limit 1  """ % (str(phone))
    r = requests.post('https://localhost/foxtrot/v1/fql', data=finalFql, headers = {"Accept": "application/json",'content-type': 'application/json','Authorization':''})
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
                    convrow = dict((k.lower().strip().replace(' ', '_'), v.strip()) for k,v in row.iteritems())
                    if(convrow.has_key("")):
                        del convrow[""]
                    convrow['wfSource'] = 'delhi_hq_monitoring_csv'
                    endTime = epoch_time(convrow['end_date'])
                    convrow['maxDays'] = day_diff(now(),endTime)
                    convrow['endTime'] = endTime
                    body = { 'id' : 'delhi_hq_monitoring_csv', 'body' : convrow, 'apiPath' : csvFileName }
                    print('Queuing job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                    jobQueue.put(json.dumps(body))
                    print('Queued job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                except:
                    print('Error processing row: ' + str(row))
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
    w = existing_workflow(mobileNumber)
    if(w is None):
        if(trigger_new_workflow(payload,mobileNumber)):
            jobQueue.ack(payload)
        else:
            jobQueue.ack_failed(payload)
    else:
        if(update_workflow(w, payloadDict, mobileNumber)):
            jobQueue.ack(payload)
        else:
            jobQueue.ack_failed(payload)

shutil.rmtree('covid-monitoring')
print('Processing complete')