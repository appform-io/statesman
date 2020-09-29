import MySQLdb
import datetime
import glob
import json
import os
import requests
import time

STATESMAN_WORKFLOW_GET_URL = "http://127.0.0.1/v1/housekeeping/debug/workflow/{}"
HEADERS = {"Content-Type": "application/json"}
FD_DOMAIN_DATA = {
    "telemeds": {
        "statesman_recon_url" : "https://127.0.0.1/callbacks/FRESHDESK",
        "fd_url": "https://127.0.0.1/api/v2/tickets/{}",
        "fd_headers": {
            "Content-Type": "application/json",
            "Authorization": "Basic =="
        }
    },
    "covidwatch": {
        "statesman_recon_url" : "https://127.0.0.1/callbacks/COVIDWATCH_FRESHDESK",
        "fd_url": "https://127.0.0.1/api/v2/tickets/{}",
        "fd_headers": {
            "Content-Type": "application/json",
            "Authorization": "Basic =="
        }
    }
}

HOST = '127.0.0.1'
USER = 'root'
PASSWORD = ''
DATABASE = 'statesman_db_'
SHARDS = 16

PENDING_WORKFLOW_SQL = """ select workflow_id,data from workflow_instances where current_state IN ('CALL_NEEDED','CALL_REQUIRED','CALL_NEEDED_MENTAL_HEALTH','CALL_NEEDED_COVID_POSITIVE','DOCTOR_FOLLOW','FINAL_DOCTOR_FOLLOW','SYMPTOMS_DOCTOR_FOLLOW','INTENDED_PLASMA_DONATION','HI_ONBOARD_DOCTOR_FOLLOW','HI_DAILY_FOLLOWUP','HI_SYMPTOMS_DOCTOR_FOLLOW','HI_SYMPTOMS_DOCTOR_EMERGENCY') and updated > '2020-09-05 05:00:00' and  updated < DATE_SUB(NOW(), INTERVAL 1 HOUR) """


############ DATE HELPER ###########

def formatted_date_time(epoch):
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(epoch / 1000))


def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%Y-%m-%dT%H:%M:%SZ"))) + 19800) * 1000


def str_current_time():
    return datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%S")


#######################  MYSQL HELPER ##########################

def connection(database):
    db = MySQLdb.connect(HOST, USER, PASSWORD, database)
    return db


def execute_query(sql):
    result = []
    for i in range(SHARDS):
        mainDb = connection(DATABASE + str(i))  # getting connection
        cursor = mainDb.cursor(MySQLdb.cursors.SSCursor)  # fetch data in bactch not all at once
        # executing Query
        cursor.execute(sql)
        for row in cursor:
            result.append(row)
        print("INFO COMPLETED SHARD: " + str(i))
        mainDb.commit()
        cursor.close()
        mainDb.close()
    return result


############ FRESHDESK HELPER ##########

def fetch_freshdesk_ticket(ticket_domain, ticket_id):
    response = requests.get(url=FD_DOMAIN_DATA[ticket_domain]['fd_url'].format(ticket_id), headers=FD_DOMAIN_DATA[ticket_domain]['fd_headers'])
    if response.status_code == 200:
        return response.json()
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


def recon_workflow(ticket_domain,payload):
    try:
        print(json.dumps(payload))
        response = requests.post(url=FD_DOMAIN_DATA[ticket_domain]['statesman_recon_url'], data=json.dumps(payload), headers=HEADERS)
        print(response.text)
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
                         patient_pincode,
                         fsm_service_location,
                         fsm_appointment_end_time,
                         fsm_appointment_start_time,
                         foreign_travel_history,
                         contact,
                         category,
                         agent_email,
                         state,
                         group_name,
                         id,
                         url,
                         agent_name,
                         tags,
                         fsm_customer_signature,
                         doctor_notes,
                         ticket_type,
                         cf_hq_doctor_notes,
                         cf_delhi_patient_mobile_no,
                         cf_2nd_mobile_number,
                         cf_clinical_status,
                         cf_comoribities_diabetes,
                         cf_comorbidity_diabetic,
                         cf_comorbidity_hiv,
                         cf_comorbidity_heart_disease,
                         cf_comorbidity_hypertension,
                         cf_comorbidity_cancer,
                         cf_stepone_doctor_suggestion,
                         cf_hq_doctor_recommendation,
                         cf_is_the_above_address_correct,
                         cf_if_above_address_is_wrong_enter_correct_address,
                         cf_district,
                         cf_comorbidity_other_immunodeficiency_inducing_syndromedisease,
                         cf_tele_agent_input,
                         cf_covid_care_center_name,
                         cf_hospital_name,
                         cf_hospital,
                         cf_tele_agent_notes,
                         cf_symptom_difficulty_in_breathing,
                         cf_symptoms_breathing_issues,
                         cf_symptom_fever_above_101_since_3_days,
                         cf_symptoms_fever,
                         cf_symptom_severe_cough,
                         cf_symptoms_cough,
                         cf_symptom_diarrhoea_above_4_times_a_day,
                         cf_symptoms_diarrhoealoose_stools,
                         cf_symptom_loss_of_smell_taste,
                         cf_screeners_decision,
                         cf_if_your_current_blood_oxygen_level_is_below_95,
                         cf_is_your_current_blood_oxygen_level_below_95,
                         cf_pulse_rate_range,
                         cf_do_you_have_a_fever_today,
                         cf_do_you_have_fever_today,
                         cf_did_you_have_more_than_4_loose_motions_today,
                         cf_do_you_have_any_chest_pain_or_have_any_abnormal_sweating,
                         cf_do_you_have_chest_pain_or_abnormal_sweating,
                         cf_do_you_have_any_breathing_difficulty,
                         cf_do_you_have_breathing_difficulty,
                         cf_do_you_have_cough_or_sore_throat_today,
                         cf_plasma_counsellors_decision):
    paylaod = {
        "freshdesk_webhook": {
            "ticket_cf_patient_language": patient_language,
            "ticket_status": status,
            "ticket_cf_fsm_contact_name": contact_name,
            "ticket_cf_patient_age": patient_age,
            "ticket_cf_patient_name": patient_name,
            "ticket_cf_fsm_phone_number": patient_number,
            "ticket_cf_patient_gender": patient_gender,
            "ticket_cf_pin_code": patient_pincode,
            "ticket_cf_fsm_service_location": fsm_service_location,
            "ticket_cf_fsm_appointment_end_time": fsm_appointment_end_time,
            "ticket_cf_fsm_appointment_start_time": fsm_appointment_start_time,
            "ticket_cf_foreign_travel_history": foreign_travel_history,
            "ticket_cf_contact": contact,
            "ticket_cf_category": category,
            "ticket_agent_email": agent_email,
            "ticket_cf_state": state,
            "ticket_group_name": group_name,
            "ticket_id": id,
            "ticket_url": url,
            "ticket_agent_name": agent_name,
            "ticket_tags": tags,
            "ticket_cf_fsm_customer_signature": fsm_customer_signature,
            "ticket_cf_doctor_notes": doctor_notes,
            "ticket_ticket_type": ticket_type,
            "ticket_cf_hq_doctor_notes": cf_hq_doctor_notes,
            "ticket_cf_delhi_patient_mobile_no": cf_delhi_patient_mobile_no,
            "ticket_cf_2nd_mobile_number": cf_2nd_mobile_number,
            "ticket_cf_clinical_status": cf_clinical_status,
            "ticket_cf_comoribities_diabetes": cf_comoribities_diabetes,
            "ticket_cf_comorbidity_diabetic": cf_comorbidity_diabetic,
            "ticket_cf_comorbidity_hiv": cf_comorbidity_hiv,
            "ticket_cf_comorbidity_heart_disease": cf_comorbidity_heart_disease,
            "ticket_cf_comorbidity_hypertension": cf_comorbidity_hypertension,
            "ticket_cf_comorbidity_cancer": cf_comorbidity_cancer,
            "ticket_cf_stepone_doctor_suggestion": cf_stepone_doctor_suggestion,
            "ticket_cf_hq_doctor_recommendation": cf_hq_doctor_recommendation,
            "ticket_cf_is_the_above_address_correct": cf_is_the_above_address_correct,
            "ticket_cf_if_above_address_is_wrong_enter_correct_address": cf_if_above_address_is_wrong_enter_correct_address,
            "ticket_cf_district": cf_district,
            "ticket_cf_comorbidity_other_immunodeficiency_inducing_syndromedisease": cf_comorbidity_other_immunodeficiency_inducing_syndromedisease,
            "ticket_cf_tele_agent_input": cf_tele_agent_input,
            "ticket_cf_covid_care_center_name": cf_covid_care_center_name,
            "ticket_cf_hospital_name": cf_hospital_name,
            "ticket_cf_hospital": cf_hospital,
            "ticket_cf_tele_agent_notes": cf_tele_agent_notes,
            "ticket_cf_symptom_difficulty_in_breathing": cf_symptom_difficulty_in_breathing,
            "ticket_cf_symptoms_breathing_issues": cf_symptoms_breathing_issues,
            "ticket_cf_symptom_fever_above_101_since_3_days": cf_symptom_fever_above_101_since_3_days,
            "ticket_cf_symptoms_fever": cf_symptoms_fever,
            "ticket_cf_symptom_severe_cough": cf_symptom_severe_cough,
            "ticket_cf_symptoms_cough": cf_symptoms_cough,
            "ticket_cf_symptom_diarrhoea_above_4_times_a_day": cf_symptom_diarrhoea_above_4_times_a_day,
            "ticket_cf_symptoms_diarrhoealoose_stools": cf_symptoms_diarrhoealoose_stools,
            "ticket_cf_symptom_loss_of_smell_taste": cf_symptom_loss_of_smell_taste,
            "ticket_cf_screeners_decision": cf_screeners_decision,
            "ticket_cf_if_your_current_blood_oxygen_level_is_below_95":cf_if_your_current_blood_oxygen_level_is_below_95,
            "ticket_cf_is_your_current_blood_oxygen_level_below_95":cf_is_your_current_blood_oxygen_level_below_95,
            "ticket_cf_pulse_rate_range":cf_pulse_rate_range,
            "ticket_cf_do_you_have_a_fever_today":cf_do_you_have_a_fever_today,
            "ticket_cf_do_you_have_fever_today":cf_do_you_have_fever_today,
            "ticket_cf_did_you_have_more_than_4_loose_motions_today":cf_did_you_have_more_than_4_loose_motions_today,
            "ticket_cf_do_you_have_any_chest_pain_or_have_any_abnormal_sweating":cf_do_you_have_any_chest_pain_or_have_any_abnormal_sweating,
            "ticket_cf_do_you_have_chest_pain_or_abnormal_sweating":cf_do_you_have_chest_pain_or_abnormal_sweating,
            "ticket_cf_do_you_have_any_breathing_difficulty":cf_do_you_have_any_breathing_difficulty,
            "ticket_cf_do_you_have_breathing_difficulty":cf_do_you_have_breathing_difficulty,
            "ticket_cf_do_you_have_cough_or_sore_throat_today":cf_do_you_have_cough_or_sore_throat_today,
            "ticket_cf_plasma_counsellors_decision": cf_plasma_counsellors_decision
        }
    }
    return paylaod


def recon_required_based_on_workflow(workflow):
    return workflow.has_key("dataObject") and workflow["dataObject"].has_key("currentState") and \
           workflow["dataObject"]["currentState"]["name"] == "CALL_NEEDED"


def recon_required_based_on_ticket_details(ticket_details):
    return ticket_details is not None and get_or_default(ticket_details, "status", 0) >= 4


def statusString(status):
    if(status == 2):
        return "open"
    elif(status == 3):
        return "pending"
    elif(status == 4):
        return "resolved"
    elif(status == 5):
        return "closed"
    else:
        return ""

def create_recon_payload_from_ticket_details(ticket_details, worklow_id):
    cf = ticket_details['custom_fields'] if ticket_details.has_key('custom_fields') else {}
    return create_recon_payload(id=get_or_default(ticket_details, "id", ""),
                                url=get_or_default(ticket_details, "url", ""),
                                tags=','.join(get_or_default(ticket_details, "tags", [])),
                                group_name=str(get_or_default(ticket_details, "group_id", "")),
                                agent_email=get_or_default(ticket_details, "agent_email", ""),
                                agent_name=get_or_default(ticket_details, "agent_name", ""),
                                status=statusString(get_or_default(ticket_details, "status", 4)),
                                ticket_type=get_or_default(ticket_details, "type", ""),
                                patient_language=get_or_default(cf, "cf_patient_language", ""),
                                contact_name=get_or_default(cf, "cf_fsm_contact_name", ""),
                                patient_age=get_or_default(cf, "cf_patient_age", ""),
                                patient_name=get_or_default(cf, "cf_patient_name", ""),
                                patient_number=get_or_default(cf, "cf_fsm_phone_number", ""),
                                patient_gender=get_or_default(cf, "cf_patient_gender", ""),
                                patient_pincode=get_or_default(cf, "cf_pin_code", ""),
                                state=get_or_default(cf, "cf_state", ""),
                                fsm_service_location=get_or_default(cf, "cf_fsm_service_location", ""),
                                fsm_appointment_end_time=get_or_default(cf, "cf_fsm_appointment_end_time", ""),
                                fsm_appointment_start_time=get_or_default(cf, "cf_fsm_appointment_start_time", ""),
                                foreign_travel_history=get_or_default(cf, "cf_foreign_travel_history", ""),
                                contact=get_or_default(cf, "cf_contact", ""),
                                category=get_or_default(cf, "cf_category", ""),
                                doctor_notes=get_or_default(cf, "cf_doctor_notes", ""),
                                cf_hq_doctor_notes=get_or_default(cf, "cf_hq_doctor_notes", ""),
                                cf_delhi_patient_mobile_no=get_or_default(cf, "cf_delhi_patient_mobile_no", ""),
                                cf_2nd_mobile_number=get_or_default(cf, "cf_2nd_mobile_number", ""),
                                cf_clinical_status=get_or_default(cf, "cf_clinical_status", ""),
                                cf_comoribities_diabetes=get_or_default(cf, "cf_comoribities_diabetes", ""),
                                cf_comorbidity_diabetic=get_or_default(cf, "cf_comorbidity_diabetic", ""),
                                cf_comorbidity_hiv=get_or_default(cf, "cf_comorbidity_hiv", ""),
                                cf_comorbidity_heart_disease=get_or_default(cf, "cf_comorbidity_heart_disease", ""),
                                cf_comorbidity_hypertension=get_or_default(cf, "cf_comorbidity_hypertension", ""),
                                cf_comorbidity_cancer=get_or_default(cf, "cf_comorbidity_cancer", ""),
                                cf_stepone_doctor_suggestion=get_or_default(cf, "cf_stepone_doctor_suggestion", ""),
                                cf_hq_doctor_recommendation=get_or_default(cf, "cf_hq_doctor_recommendation", ""),
                                cf_is_the_above_address_correct=get_or_default(cf, "cf_is_the_above_address_correct", ""),
                                cf_if_above_address_is_wrong_enter_correct_address=get_or_default(cf, "cf_if_above_address_is_wrong_enter_correct_address", ""),
                                cf_district=get_or_default(cf, "cf_district", ""),
                                cf_comorbidity_other_immunodeficiency_inducing_syndromedisease=get_or_default(cf, "cf_comorbidity_other_immunodeficiency_inducing_syndromedisease", ""),
                                cf_tele_agent_input=get_or_default(cf, "cf_tele_agent_input", ""),
                                cf_covid_care_center_name=get_or_default(cf, "cf_covid_care_center_name", ""),
                                cf_hospital_name=get_or_default(cf, "cf_hospital_name", ""),
                                cf_hospital=get_or_default(cf, "cf_hospital", ""),
                                cf_tele_agent_notes=get_or_default(cf, "cf_tele_agent_notes", ""),
                                cf_symptom_difficulty_in_breathing=get_or_default(cf, "cf_symptom_difficulty_in_breathing", ""),
                                cf_symptoms_breathing_issues=get_or_default(cf, "cf_symptoms_breathing_issues", ""),
                                cf_symptom_fever_above_101_since_3_days=get_or_default(cf, "cf_symptom_fever_above_101_since_3_days", ""),
                                cf_symptoms_fever=get_or_default(cf, "cf_symptoms_fever", ""),
                                cf_symptom_severe_cough=get_or_default(cf, "cf_symptom_severe_cough", ""),
                                cf_symptoms_cough=get_or_default(cf, "cf_symptoms_cough", ""),
                                cf_symptom_diarrhoea_above_4_times_a_day=get_or_default(cf, "cf_symptom_diarrhoea_above_4_times_a_day", ""),
                                cf_symptoms_diarrhoealoose_stools=get_or_default(cf, "cf_symptoms_diarrhoealoose_stools", ""),
                                cf_symptom_loss_of_smell_taste=get_or_default(cf, "cf_symptom_loss_of_smell_taste", ""),
                                cf_plasma_counsellors_decision=get_or_default(cf, "cf_plasma_counsellors_decision", ""),
                                cf_screeners_decision=get_or_default(cf, "cf_screeners_decision", ""),
                                cf_if_your_current_blood_oxygen_level_is_below_95=get_or_default(cf,"cf_if_your_current_blood_oxygen_level_is_below_95",""),
                                cf_is_your_current_blood_oxygen_level_below_95=get_or_default(cf,"cf_is_your_current_blood_oxygen_level_below_95",""),
                                cf_pulse_rate_range=get_or_default(cf,"cf_pulse_rate_range",""),
                                cf_do_you_have_a_fever_today=get_or_default(cf,"cf_do_you_have_a_fever_today",""),
                                cf_do_you_have_fever_today=get_or_default(cf,"cf_do_you_have_fever_today",""),
                                cf_did_you_have_more_than_4_loose_motions_today=get_or_default(cf,"cf_did_you_have_more_than_4_loose_motions_today",""),
                                cf_do_you_have_any_chest_pain_or_have_any_abnormal_sweating=get_or_default(cf,"cf_do_you_have_any_chest_pain_or_have_any_abnormal_sweating",""),
                                cf_do_you_have_chest_pain_or_abnormal_sweating=get_or_default(cf,"cf_do_you_have_chest_pain_or_abnormal_sweating",""),
                                cf_do_you_have_any_breathing_difficulty=get_or_default(cf,"cf_do_you_have_any_breathing_difficulty",""),
                                cf_do_you_have_breathing_difficulty=get_or_default(cf,"cf_do_you_have_breathing_difficulty",""),
                                cf_do_you_have_cough_or_sore_throat_today=get_or_default(cf,"cf_do_you_have_cough_or_sore_throat_today",""),
                                fsm_customer_signature=worklow_id)


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


############ STATESMAN DB BASED RECON ###########


def statesman_db_based_recon():
    result = execute_query(PENDING_WORKFLOW_SQL)
    for row in result:
        try:
            workflow_id = row[0]
            workflow_data = json.loads(row[1])
            if (workflow_data.has_key('data') and workflow_data['data'].has_key('freshDeskActionCall') and
                    workflow_data['data']['freshDeskActionCall'].has_key('ticketId')):
                ticket_id = workflow_data['data']['freshDeskActionCall']['ticketId']
                ticket_domain = workflow_data['data']['freshDeskActionCall']['domain'] if workflow_data['data']['freshDeskActionCall'].has_key('domain') else 'telemeds'
                print(workflow_id + "," + ticket_id+","+ ticket_domain)
                ticket_details = fetch_freshdesk_ticket(ticket_domain, ticket_id)
                if(recon_required_based_on_ticket_details(ticket_details)):
                    payload = create_recon_payload_from_ticket_details(ticket_details, workflow_id)
                    recon_workflow(ticket_domain,payload)
        except Exception, e:
            print("Error for row:" + ','.join(row) + " Error" + str(e))

statesman_db_based_recon()