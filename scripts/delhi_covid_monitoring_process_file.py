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

scanpath='/home/ganesh/upload_covid_monitoring'
processedPath='/home/ganesh/processed_covid_monitoring'
rows = []
csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ('covid-monitoring')


def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000


def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))



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
    mobileNumber = str(json.loads(payload)['body']['mobile_number'])
    for i in range(3):
        r = requests.post('http://localhost:8080/callbacks/ingress/raw/delhi_hq_monitoring_csv', data=payload, headers = {'content-type': 'application/json'})
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
shutil.rmtree('covid-monitoring')
print('Processing complete')