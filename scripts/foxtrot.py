from __future__ import division

import collections
import json
import requests
import time

FOXTROT_URL = "http://127.0.0.1/foxtrot/v1/analytics"
BATCH_SIZE = 10000
HEADERS = {
    "Content-Type": "application/json"
}

OUTPUT_KEY_NAMES = [
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


def write_as_csv_line_with_keys(file_handler, keys, row):
    line = ','.join([str(row.get(x) if row.has_key(x) else '') for x in keys])
    file_handler.write(line + "\n")


def write_as_csv_line(file_handler, row):
    line = ','.join([str(x) for x in row])
    file_handler.write(line + "\n")


def formatted_time(epoch):
    return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(epoch))


def flatten(d, parent_key='', sep='.'):
    items = []
    for k, v in d.items():
        new_key = parent_key + sep + k if parent_key else k
        if isinstance(v, collections.MutableMapping):
            items.extend(flatten(v, new_key, sep=sep).items())
        else:
            items.append((new_key, v))
    return dict(items)


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


def generate_report(table, filters, keys, file_name, start_time, end_time):
    file_handler = open(file_name, "w")
    write_as_csv_line(file_handler, OUTPUT_KEY_NAMES)
    execute_foxtrot_query_paginated(table, filters, keys, file_handler, start_time, end_time)
    file_handler.close()


def execute_foxtrot_query_paginated(table, filters, keys, file_handler, start_time, end_time):
    start = 0
    while (True):
        response = execute_query_foxtrot(table, filters, start, BATCH_SIZE, start_time, start_time + end_time)
        current_batch_size = 0
        if 'documents' in response:
            for document in response['documents']:
                document = flatten(document)
                write_as_csv_line_with_keys(file_handler, keys, document)
                current_batch_size += 1
                file_handler.flush()
        if (current_batch_size == 0 or current_batch_size < BATCH_SIZE):
            break
        start = start + current_batch_size
