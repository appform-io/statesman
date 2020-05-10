import datetime
import glob
import os
import requests
import time
import urllib
import json
FROM = 1588735390129
TO = 1588771374705

FILE_PATH_PREFIX = "/var/tmp/reports/FRESHDESK_"
FRESHDESK_URL = "https://127.0.0.1/api/v2/tickets?order_by=updated_at&order_type=asc&per_page=100&page={}&updated_since={}"
HEADERS = {
    "Content-Type": "application/json",
    "Authorization": ""
}


############ DATE HELPER ###########

def formatted_date_time(epoch):
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.localtime(epoch / 1000))


def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%Y-%m-%dT%H:%M:%SZ")))) * 1000


def str_current_time():
    return datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")


############ FRESHDESK HELPER ##########

def fetch_freshdesk_ticket(ticket_id):
    response = requests.get(url=FRESHDESK_TICKET_URL.format(ticket_id), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return None


def fetch_next_freshdesk_tickets(page, from_str_time):
    response = requests.get(url=FRESHDESK_URL.format(page, urllib.quote(from_str_time)), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return []


def fetch_next_freshdesk_tickets_till_date_changes(from_time_till_date_change):
    print("FETCHING from date :"+formatted_date_time(from_time_till_date_change))
    page = 1
    last_ticket_time_till_date_change = from_time_till_date_change
    all_tickets = []
    while last_ticket_time_till_date_change == from_time_till_date_change:
        tickets = fetch_next_freshdesk_tickets(str(page), formatted_date_time(from_time_till_date_change))
        if (len(tickets) == 0):
            break
        page += 1
        last_ticket_time_till_date_change = epoch_time(tickets[-1]["updated_at"])
        all_tickets = tickets + all_tickets
    return all_tickets


def fetch_freshdesk_tickets(from_time, to_time):
    all_tickets = []
    while from_time < to_time:
        tickets = fetch_next_freshdesk_tickets_till_date_changes(from_time)
        if (len(tickets) == 0):
            break
        last_ticket_time = epoch_time(tickets[-1]["updated_at"])
        if (last_ticket_time == from_time):
            from_time = last_ticket_time + 1000  # adding one sec
        else:
            from_time = last_ticket_time
        all_tickets = tickets + all_tickets
    tickets_dict = dict()
    for ticket in all_tickets:
        tickets_dict[ticket['id']] = ticket
    return tickets_dict.values()           #returning uniq list


############ COMMON UTILS #############

def delete_path(file_path):
    try:
        os.remove(file_path)
    except OSError:
        print("Error while deleting file:" + file_path)


def delete_match(file_path_regex):
    file_list = glob.glob(file_path_regex)
    for file_path in file_list:
        delete_path(file_path)


def get_or_default(data, key, default_value):
    return data[key] if data.has_key(key) else default_value


def filter_resolved(tickets):
    resolved_tickets = []
    for ticket in tickets:
        if (ticket["status"] == 4 or ticket["status"] == 5):
            resolved_tickets.append(ticket)
    return resolved_tickets


def resolved_tickets():
    resolved_tickets  = filter_resolved(fetch_freshdesk_tickets(FROM, TO))
    for resolved_ticket in resolved_tickets:
        print json.dumps(resolved_ticket)
    return resolved_tickets


resolved_tickets()