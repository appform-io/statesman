import requests
import time
import urllib

FROM = 1586375271662
TO = 1586460257299

STATESMAN_URL = "http://a254946ea791611eabe57061fcb77c4f-40500932.ap-south-1.elb.amazonaws.com:8080/v1/housekeeping/debug/workflow/{}"
FRESHDESK_URL = "https://telemeds.freshdesk.com/api/v2/tickets?order_by=updated_at&order_type=asc&per_page=100&page={}&updated_since={}"
HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Basic YXNnYW5lc2gyMzRAZ21haWwuY29tOnRlbGVtZWQxOQ=="
}


def formatted_date_time(epoch):
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(epoch / 1000))


def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%Y-%m-%dT%H:%M:%SZ"))) + 19800) * 1000


def fetch_next_freshdesk_tickets(page, from_str_time):
    response = requests.get(url=FRESHDESK_URL.format(page, urllib.quote(from_str_time)), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return []


def fetch_next_freshdesk_tickets_till_date_changes(from_time_till_date_change):
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
    return all_tickets


def filter_resolved(tickets):
    resolved_tickets = []
    for ticket in tickets:
        if (ticket["status"] == 4):
            resolved_tickets.append(ticket)
    return resolved_tickets

def get_workflow(workflow_id):
    response = requests.get(url=STATESMAN_URL.format(workflow_id), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return {}

def recon_required(workflow):
    return workflow.has_key("currentState") and workflow["currentState"]["currentState"] == "CALL_NEEDED"

for resolved_ticket in filter_resolved(fetch_freshdesk_tickets(FROM, TO)):
    print resolved_ticket
    if(resolved_ticket.has_key("custom_fields") and resolved_ticket["custom_fields"].has_key("cf_fsm_customer_signature") and resolved_ticket["custom_fields"]["cf_fsm_customer_signature"] != ''):
        workflow = get_workflow(resolved_ticket["custom_fields"]["cf_fsm_customer_signature"])
        print workflow
        print recon_required(workflow)