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

scanpath='upload_end_all_workflows'
processedPath='processed_end_all_workflows'
rows = []
csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ('end-all-workflows')


def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000


def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))


def get_workflow(phone):
    workflows =list()
    phoneStr = str(phone)
    fql = """select distinct(eventData.workflowId) from statesman where eventData.data.mobile_number = '%s' or eventData.data.contact_number = '%s' or eventData.data.phone = '%s' """ % (phoneStr,phoneStr,phoneStr)
    r = requests.post('https://localhost/foxtrot/v1/fql', data=fql, headers = {"Accept": "application/json",'content-type': 'application/json','Authorization':''})
    if(r.status_code != 200):
        return workflows
    for row in r.json()['rows']:
        workflows.append(row['eventData.workflowId'])
    return workflows


for csvFileName in csvFileNames:
    fqFileName=scanpath + '/' + csvFileName
    fqDestFilename=processedPath + '/' + csvFileName
    try:
        with open(fqFileName) as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                try:
                    convrow = dict((k.lower().strip().replace(' ', '_'), v.strip()) for k,v in row.iteritems())
                    workflows = get_workflow(convrow['mobile_number'])
                    for w in workflows:
                        jobData = {"mobile_number":convrow['mobile_number'],
                                   "workflowId": w,
                                   "state": "END",
                                   "terminal": True}
                        print('Queuing job workflow: ' + w)
                        jobQueue.put(json.dumps(jobData))
                        print('Queued job workflow: ' + w)
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
    jobData = json.loads(payload)
    mobileNumber = str(jobData['mobile_number'])
    url = 'http://localhost:8080/v1/housekeeping/update/workflow/%s/state/' % (jobData['workflowId'])
    for i in range(3):
        r = requests.put(url, data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for mobileNumber: ' + mobileNumber)
            jobQueue.ack(payload)
            break
        else:
            print(str(i) + ': could not post data for mobileNumber: ' + mobileNumber + ' status:' + str(r.status_code))
            if i == 2:
                print('Failing for mobileNumber: ' + mobileNumber)
                jobQueue.ack_failed(payload)
            else:
                print('Retrying for mobileNumber: ' + mobileNumber)
                jobQueue.nack(payload)
                break
shutil.rmtree('end-all-workflows')
print('Processing complete')