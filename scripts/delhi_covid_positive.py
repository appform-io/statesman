#!/usr/bin/env python
from os import listdir
from os.path import isfile, join
import csv
import json
import persistqueue
import requests
import shutil
scanpath='uploads'
processedPath='processed'
rows = []
csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ('csv-processor')
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
                    convrow['wfSource'] = 'delhi_covid_positive_csv'
                    print('Queued job: ' + convrow['icmr_id'])
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
    icmrId = str(json.loads(payload)['body']['icmr_id'])
    for i in range(3):
        r = requests.post('http://localhost:8080/callbacks/ingress/raw/delhi_csv', data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for icmr id: ' + icmrId)
            jobQueue.ack(payload)
            break
        else:
            print(str(i) + ': could not post data for icmr id: ' + icmrId + ' status:' + str(r.status_code))
            if i == 2:
                print('Failing for icmr id: ' + icmrId)
                jobQueue.ack_failed(payload)
            else:
                print('Retrying for icmr id: ' + icmrId)
                jobQueue.nack(payload)
                break
shutil.rmtree('csv-processor')
print('Processing complete')