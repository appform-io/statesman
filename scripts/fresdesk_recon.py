import csv
import datetime
import json
import requests
import time
import urllib

FROM = 1586375271662
TO = 1586460257299

STATESMAN_WORKFLOW_GET_URL = "http://127.0.0.1:8080/v1/housekeeping/debug/workflow/{}"
STATESMAN_RECON_URL = "https://127.0.0.1/callbacks/FRESHDESK"
FRESHDESK_URL = "https://127.0.0.1/api/v2/tickets?order_by=updated_at&order_type=asc&per_page=100&page={}&updated_since={}"
FRESHDESK_TICKETS_FILE_URL = "https://127.0.0.1/reports/scheduled_exports/4830771586540088/download_file.json"
HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Basic =="
}


############ DATE HELPER ###########

def formatted_date_time(epoch):
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(epoch / 1000))


def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%Y-%m-%dT%H:%M:%SZ"))) + 19800) * 1000


def str_current_time():
    return datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")


############ FRESHDESK HELPER ##########

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


def download_tickets_last_hour():
    file_path = "/var/tmp/reports/FRESHDESK_" + str_current_time()
    response = requests.get(url=FRESHDESK_TICKETS_FILE_URL, headers=HEADERS)
    if response.status_code == 200:
        url = response.json()["export"]["url"]
        file_response = requests.get(url=url)
        file_handler = open(file_path, 'wb')
        file_handler.write(file_response.content)
        file_handler.close()
        return file_path
    else:
        return None


############ STATESMAN HELPER ##########


def get_workflow(workflow_id):
    try:
        response = requests.get(url=STATESMAN_WORKFLOW_GET_URL.format(workflow_id), headers=HEADERS)
        if response.status_code == 200:
            return response.json()
        else:
            return {}
    except:
        pass
    return {}


def recon_workflow(payload):
    try:
        print(json.dumps(payload))
        response = requests.post(url=STATESMAN_RECON_URL, data=json.dumps(payload), headers=HEADERS)
        if response.status_code == 200:
            return response.json()
        else:
            return {}
    except:
        pass
    return {}


def create_recon_payload(patient_language,
                         status,
                         contact_name,
                         patient_age,
                         patient_name,
                         patient_number,
                         patient_gender,
                         foreign_travel_history,
                         contact,
                         agent_email,
                         state,
                         group_name,
                         id,
                         url,
                         agent_name,
                         tags,
                         fsm_customer_signature,
                         ticket_type):
    paylaod = {
        "freshdesk_webhook": {
            "ticket_cf_patient_language": patient_language,
            "ticket_status": status,
            "ticket_cf_fsm_contact_name": contact_name,
            "ticket_cf_patient_age": patient_age,
            "ticket_cf_patient_name": patient_name,
            "ticket_cf_fsm_phone_number": patient_number,
            "ticket_cf_patient_gender": patient_gender,
            "ticket_cf_foreign_travel_history": foreign_travel_history,
            "ticket_cf_contact": contact,
            "ticket_agent_email": agent_email,
            "ticket_cf_state": state,
            "ticket_group_name": group_name,
            "ticket_id": id,
            "ticket_url": url,
            "ticket_agent_name": agent_name,
            "ticket_tags": tags,
            "ticket_cf_fsm_customer_signature": fsm_customer_signature,
            "ticket_ticket_type": ticket_type
        }
    }
    return paylaod


def recon_required(workflow):
    return workflow.has_key("dataObject") and workflow["dataObject"].has_key("currentState") and workflow["dataObject"]["currentState"]["name"] == "CALL_NEEDED"


############ COMMON UTILS #############

def get_or_default(data, key, default_value):
    return data[key] if data.has_key(key) else default_value


def filter_resolved(tickets):
    resolved_tickets = []
    for ticket in tickets:
        if (ticket["status"] == 4 or ticket["status"] == 5):
            resolved_tickets.append(ticket)
    return resolved_tickets


############ API BASED RECON ###########


def api_based_recon():
    for resolved_ticket in filter_resolved(fetch_freshdesk_tickets(FROM, TO)):
        print resolved_ticket
        if (resolved_ticket.has_key("custom_fields") and resolved_ticket["custom_fields"].has_key(
                "cf_fsm_customer_signature") and resolved_ticket["custom_fields"][
            "cf_fsm_customer_signature"] != ''):
            workflow = get_workflow(resolved_ticket["custom_fields"]["cf_fsm_customer_signature"])
            recon_required(workflow)


############ FILE BASED RECON ###########

def create_recon_payload_from_csv_line(row):
    return create_recon_payload(patient_language=get_or_default(row, "Patient Language", ""),
                                status=get_or_default(row, "Status", ""),
                                contact_name=get_or_default(row, "Contact", "Contact Id"),
                                patient_age=get_or_default(row, "Patient Age", ""),
                                patient_name=get_or_default(row, "Patient Name", ""),
                                patient_number=get_or_default(row, "Phone number", ""),
                                patient_gender=get_or_default(row, "Patient Gender", ""),
                                foreign_travel_history=get_or_default(row, "Foreign Travel History", ""),
                                contact=get_or_default(row, "Contact", ""),
                                agent_email=get_or_default(row, "Agent Email", ""),
                                state=get_or_default(row, "State", ""),
                                group_name=get_or_default(row, "Group", ""),
                                id=get_or_default(row, "Ticket Id", ""),
                                url=get_or_default(row, "Ticket URL", ""),
                                agent_name=get_or_default(row, "Agent", ""),
                                tags=get_or_default(row, "Tags", ""),
                                fsm_customer_signature=get_or_default(row, "Customer's signature", ""),
                                ticket_type=get_or_default(row, "Type", ""))


def file_based_recon():
    file_name = download_tickets_last_hour()
    if (file_name is not None):
        input_file = csv.DictReader(open(file_name))
        for row in input_file:
            if (row.has_key("Customer's signature") and (row['Status'] == 'Resolved' or row['Status'] == 'Closed')):
                workflow = get_workflow(row["Customer's signature"])
                if (recon_required(workflow)):
                    payload = create_recon_payload_from_csv_line(row)
                    recon_workflow(payload)


file_based_recon()
