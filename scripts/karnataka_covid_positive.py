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
jobQueue = persistqueue.UniqueAckQ('ka-covid-csv-processor')
fieldsRequired = ['repeat_sample_negative_on', 'symptom_status', 'geotag_ward', 'date_of_hospitalization', 'created_on', 'district_of_residence', 'test_method', 'owner', 'date_of_sample_collection', 'secondary_contacts', 'hospitalization_type', 'contact_number', 'modified_by', 'flight_details', 'geotag_address', 'created_by', 'record_status', 'patient_name', 'symptoms', 'email', 'modified_on', 'district_p_code', 'date_of_travel', 'date_of_discharge', 'icmr_address', 'description', 'state_p_code', 'confirmation_date', 'patient_id', 'hospitalization_at', 'state_of_residence', 'date_of_death', 'icmr_ward', 'symptoms_condition', 'geotag_zone', 'remarks', 'status_reason', 'result_declared_on', 'admitted_district', 'primary_contacts', 'gender', 'age', 'current_status', 'record_created_on', 'icmr_id', 'icmr_zone', 'status', 'location_address', 'repeat_sample_sent_on', 'laboratory_code', 'laboratory_name', 'date_of_onset_of_symptoms']

for csvFileName in csvFileNames:
    fqFileName=scanpath + '/' + csvFileName
    print(fqFileName)
    fqDestFilename=processedPath + '/' + csvFileName
    try:
        with open(fqFileName) as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                try:
                    convrow_all = dict((k.lower().strip().replace(' ', '_'), v.strip()) for k,v in row.iteritems())
                    if(convrow_all.has_key("")):
                        del convrow_all[""]
                    convrow = {}
                    for k in convrow_all.keys():
                        if k in fieldsRequired: 
                            convrow[k] = convrow_all[k]	
                    convrow['wfSource'] = 'karnataka_covid_positive_csv'
                    jobQueue.put(json.dumps({ 'body' : convrow } ))
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
        r = requests.post('http://localhost:8080/callbacks/ingress/raw/karnataka_csv', data=payload, headers = {'content-type': 'application/json'})
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
shutil.rmtree('ka-covid-csv-processor')
print('Processing complete')
