import csv
import glob
import os
import requests
import smtplib
import time
import urllib.parse
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from io import StringIO

FRESHDESK_URL = "https://127.0.0.1/api/v2/tickets?order_by=updated_at&order_type=asc&per_page=100&page={}&updated_since={}"
HEADERS = {
    "Content-Type": "application/json",
    "Authorization": "Basic =="
}

SMTP_HOST = "127.0.0.1"
SMTP_PORT = 25
SMTP_LOGIN_USER = ""
SMTP_LOGIN_PASSWORD = ""
EMAIL_SENDER = "noreply@gmail.com"
STATE_EMAIL_LIST = {
    "karnataka": ["me@gmail.com"],
    "maharashtra": ["me@gmail.com"],
    "punjab": ["me@gmail.com"],
    "orissa": ["me@gmail.com"],
    "chhattisgarh": ["me@gmail.com"],
    "nagaland": ["me@gmail.com"],
    "madhya pradesh": ["me@gmail.com"]
}

FILE_PATH_PREFIX = "/var/tmp/reports/FRESHDESK_"
EMAIL_STATIC_CONTENT = """<html lang="en">
                            <body>
                            <p>Please find the attached : %s </p>
                            </body>
                            </html>
                       """


############ DATE HELPER ###########

def formatted_date_time(epoch):
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.localtime(epoch / 1000))


def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%Y-%m-%dT%H:%M:%SZ")))) * 1000


def epoch_time_till_last_hr():
    return epoch_time(time.strftime("%Y-%m-%dT%H:00:00Z", time.localtime()))


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


def remove_old_files():
    delete_match(FILE_PATH_PREFIX + "*")


def to_csv(data):
    csv_file = StringIO()
    writer = csv.DictWriter(csv_file, data[0].keys())
    writer.writeheader()
    writer.writerows(data)
    return csv_file.getvalue()


def get_or_default(data, key, default_value):
    return data[key] if key in data and data[key] is not None else default_value


def get_custom_field(ticket, field):
    return str(ticket["custom_fields"][field]) if "custom_fields" in ticket and field in ticket["custom_fields"] and \
                                                  ticket["custom_fields"][field] is not None else ""


def statusString(status):
    if (status == 2):
        return "open"
    elif (status == 3):
        return "pending"
    elif (status == 4):
        return "resolved"
    elif (status == 5):
        return "closed"
    else:
        return ""


def format_report_dict(ticket):
    created_date = get_or_default(ticket, "created_at", "T")
    updated_at = get_or_default(ticket, "updated_at", "T")
    return {
        "Request Id": str(ticket["id"]),
        "Created Date": str(created_date.split("T")[0]),
        "Created Time": str(created_date.split("T")[1] if len(created_date.split("T")) > 1 else ""),
        "IVR ID": "",
        "IVR Language": str(get_custom_field(ticket, "cf_patient_language")),
        "Phone Number": str(get_custom_field(ticket, "cf_fsm_phone_number")),
        "Location": str(get_custom_field(ticket, "cf_fsm_service_location")),
        "Pincode": str(get_custom_field(ticket, "cf_pin_code")),
        "Claimed by": "",
        "Name": str(get_custom_field(ticket, "cf_patient_name")),
        "Age": str(get_custom_field(ticket, "cf_patient_age")),
        "Sex": str(get_custom_field(ticket, "cf_patient_gender")),
        "Travel History": str(get_custom_field(ticket, "cf_foreign_travel_history")),
        "Contact History": str(get_custom_field(ticket, "cf_contact")),
        "IVR assessment": "",
        "Current Queue": "",
        "Current Status": statusString(get_or_default(ticket, "status", 100)),
        "Triage Results": str(get_or_default(ticket, "type", "")),
        "Time of Action": str(updated_at),
        "Doctor Notes": get_custom_field(ticket, "cf_doctor_notes"),
        "Last Service ID": ""
    }


def filter_resolved(tickets):
    resolved_tickets = []
    for ticket in tickets:
        if (ticket["status"] == 4 or ticket["status"] == 5):
            resolved_tickets.append(ticket)
    return resolved_tickets


############ EMAIL HELPER ##########

def attach_csv_file(file_name, data):
    return {
        "name": file_name,
        "data": to_csv(data)
    }


def send_email_with_files(to, subject, content, files, mime_sub_type='html'):
    smtp_obj = None
    msg = MIMEMultipart()
    msg['Subject'] = subject
    msg['From'] = EMAIL_SENDER
    msg['To'] = ", ".join(to)
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
        smtp_obj.sendmail(EMAIL_SENDER, to, msg.as_string())
    except smtplib.SMTPException:
        print("ERROR:While sending email to:" + ", ".join(to))
    finally:
        if (not smtp_obj is None):
            smtp_obj.quit()


############ FRESHDESK HELPER ##########

def fetch_freshdesk_ticket(ticket_id):
    response = requests.get(url=FRESHDESK_TICKET_URL.format(ticket_id), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return None


def fetch_next_freshdesk_tickets(page, from_str_time):
    response = requests.get(url=FRESHDESK_URL.format(page, urllib.parse.quote(from_str_time)), headers=HEADERS)
    if response.status_code == 200:
        return response.json()
    else:
        return []


def fetch_next_freshdesk_tickets_till_date_changes(from_time_till_date_change):
    print("FETCHING from date :" + formatted_date_time(from_time_till_date_change))
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
    return tickets_dict.values()  # returning uniq list


def state_wise_report(from_epoch, to_epoch):
    state_wise_tickets = {}
    resolved_tickets = filter_resolved(fetch_freshdesk_tickets(from_epoch, to_epoch))
    for resolved_ticket in resolved_tickets:
        state = get_custom_field(resolved_ticket, "cf_state")
        if (state is None or state == ''):
            print("Not state for ticket:" + str(resolved_ticket["id"]))
            continue
        state = state.lower()
        if (not state in state_wise_tickets):
            state_wise_tickets[state] = list()
        state_wise_tickets[state].append(format_report_dict(resolved_ticket))
    # print state_wise_tickets
    return state_wise_tickets


def send_email_report(state, data):
    report_name = state.capitalize() + " step one report"
    print("SENDING_REPORT:" + report_name)
    send_email_with_files(to=STATE_EMAIL_LIST[state],
                          subject=report_name,
                          content=EMAIL_STATIC_CONTENT % report_name,
                          files=[attach_csv_file(report_name, data)])


def run():
    to_epoch = epoch_time_till_last_hr()
    from_epoch = to_epoch - 3600000
    print(to_epoch)
    print(from_epoch)
    state_tickets_data = state_wise_report(from_epoch, to_epoch)
    for state in STATE_EMAIL_LIST.keys():
        if (state in state_tickets_data):
            send_email_report(state, state_tickets_data[state])
        else:
            print("No data for state:" + state)


if __name__ == "__main__":
    remove_old_files()
    run()
