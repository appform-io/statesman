from __future__ import division

import collections
import json
import requests
import smtplib
import time
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

EMAIL_SENDER = ''
EMAIL_RECEIVERS = ['']
SMTP_HOST = ''
SMTP_PORT = 587
SMTP_LOGIN_USER = ''
SMTP_LOGIN_PASSWORD = ''
FOXTROT_URL = "https://127.0.0.1/foxtrot/v1/analytics"
BATCH_SIZE = 10000
HEADERS = {
    "Content-Type": "application/json"
}

OUTPUT_CSV_HEADERS = [
    'Request Id',
    'Created Date',
    'Created Time',
    'IVR ID',
    'IVR Language',
    'Phone Number',
    'Location',
    'Pincode',
    'Claimed by',
    'Name  ',
    'Age',
    'Sex',
    'Travel History',
    'Contact History',
    'IVR assessment',
    'Current Queue',
    'Current Status',
    'Triage Results',
    'Time of Action',
    'Doctor Notes',
    'Last Service ID'
]


############# CSV HELPER ##########

def write_as_csv_line_with_keys(file_handler, keys, row):
    output_coulmns = list()
    for output_csv_header in OUTPUT_CSV_HEADERS:
        value = ""
        if (keys.has_key(output_csv_header) and row.has_key(keys[output_csv_header])):
            value = row[keys[output_csv_header]]
            if (output_csv_header == 'Created Date'):
                value = formatted_date(int(value) / 1000)
            elif (output_csv_header == 'Created Time'):
                value = formatted_time(int(value) / 1000)
            elif (output_csv_header == 'Time of Action'):
                value = formatted_date_time(int(value) / 1000)
        output_coulmns.append(value)
    line = ",".join(output_coulmns)
    file_handler.write(line + "\n")


def write_as_csv_line(file_handler, row):
    line = ','.join([str(x) for x in row])
    file_handler.write(line + "\n")


############ DATE HELPER ###########

def formatted_date_time(epoch):
    return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(epoch))


def formatted_date(epoch):
    return time.strftime('%Y-%m-%d', time.localtime(epoch))


def formatted_time(epoch):
    return time.strftime('%H:%M:%S', time.localtime(epoch))


############ EMAIL HELPER ##########


def send_email_with_files(subject, content, files, mime_sub_type='html'):
    smtp_obj = None
    msg = MIMEMultipart()
    msg['Subject'] = subject
    msg['From'] = EMAIL_SENDER
    msg['To'] = ", ".join(EMAIL_RECEIVERS)
    msg.attach(MIMEText(content, mime_sub_type))

    for output_file in files:
        attachment = MIMEText(output_file['data'], 'plain', 'utf-8')
        attachment.add_header('Content-Disposition', 'attachment', filename=output_file['name'])
        msg.attach(attachment)
    try:
        smtp_obj = smtplib.SMTP()
        smtp_obj.connect(SMTP_HOST, SMTP_PORT)
        smtp_obj.starttls()
        smtp_obj.login(SMTP_LOGIN_USER, SMTP_LOGIN_PASSWORD)
        smtp_obj.sendmail(EMAIL_SENDER, EMAIL_RECEIVERS, msg.as_string())
    except smtplib.SMTPException:
        print("ERROR:While sending email")
    finally:
        if (not smtp_obj is None):
            smtp_obj.quit()


############ FOXTROT HELPER ##########


def flatten(d, parent_key='', sep='.'):
    items = []
    for k, v in d.items():
        new_key = parent_key + sep + k if parent_key else k
        if isinstance(v, collections.MutableMapping):
            items.extend(flatten(v, new_key, sep=sep).items())
        else:
            items.append((new_key, v))
    return dict(items)


def count_approx_documents(table, filters, start_time, end_time):
    new_filters = list(filters)
    new_filters.append({
        "operator": "between",
        "field": "time",
        "to": end_time,
        "from": start_time
    })

    foxtrot_request = {
        "opcode": "count",
        "table": table,
        "filters": new_filters
    }
    response = requests.post(url=FOXTROT_URL, data=json.dumps(foxtrot_request), headers=HEADERS)
    return response.json()['count']


def execute_query_foxtrot(table, filters, start, count, start_time, end_time):
    new_filters = list(filters)
    new_filters.append({
        "operator": "between",
        "field": "time",
        "to": end_time,
        "from": start_time
    })
    foxtrot_request = {
        "opcode": "query",
        "table": table,
        "filters": new_filters,
        "from": start,
        "limit": count
    }
    response = requests.post(url=FOXTROT_URL, data=json.dumps(foxtrot_request), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return {}


def execute_foxtrot_query_paginated(table, filters, keys, file_handler, start_time, end_time):
    count = count_approx_documents(table, filters, start_time, end_time)
    print("Approx Event Count : " + str(count))
    time_diff = end_time - start_time
    num_of_buckets = int(count / 3000) * 3 if count > 3000 else 1
    time_duration_per_bucket = int(time_diff // num_of_buckets)
    for start_time in range(start_time, end_time, time_duration_per_bucket):
        print(formatted_date_time(start_time / 3000) + "  #####  " + formatted_date_time(
            (start_time + time_duration_per_bucket) / 3000))
        response = execute_query_foxtrot(table, filters, 0, BATCH_SIZE, start_time,
                                         start_time + time_duration_per_bucket)
        if 'documents' in response:
            for document in response['documents']:
                document = flatten(document)
                write_as_csv_line_with_keys(file_handler, keys, document)
        file_handler.flush()


def generate_report(table, filters, keys, file_name, start_time, end_time):
    file_handler = open(file_name, "w")
    write_as_csv_line(file_handler, OUTPUT_CSV_HEADERS)
    execute_foxtrot_query_paginated(table, filters, keys, file_handler, start_time, end_time)
    file_handler.close()
