from __future__ import division

import collections
import json
import requests
import time

foxtrot_url = "http://127.0.0.1/foxtrot/v1/analytics"
headers = {
    "Content-Type": "application/json"
}


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
    response = requests.post(url=foxtrot_url, data=json.dumps(foxtrot_request), headers=headers)
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
    response = requests.post(url=foxtrot_url, data=json.dumps(foxtrot_request), headers=headers)
    if response.status_code == 200:
        return response.json()
    else:
        return {}


def execute_foxtrot_query_paginated(table, filters, keys, output_key_names, file_handler, start_time, end_time):
    write_as_csv_line(file_handler, output_key_names)
    count = count_approx_documents(table, filters, start_time, end_time)
    print("Approx Event Count : " + str(count))
    time_diff = end_time - start_time
    num_of_buckets = int(count / 1000) * 5 if count > 1000 else 1
    time_duration_per_bucket = int(time_diff // num_of_buckets)

    for start_time in range(start_time, end_time, time_duration_per_bucket):
        print(formatted_time(start_time / 1000) + "  #####  " + formatted_time(
            (start_time + time_duration_per_bucket) / 1000))
        response = execute_query_foxtrot(table, filters, 0, 10000, start_time, start_time + time_duration_per_bucket)
        if 'documents' in response:
            for document in response['documents']:
                document = flatten(document)
                write_as_csv_line_with_keys(file_handler, keys, document)
        file_handler.flush()


def execute_foxtrot_query(query):
    response = requests.post(url=foxtrot_url, data=json.dumps(query), headers=headers)
    return response.json()
