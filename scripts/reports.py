import datetime

import foxtrot

start_time = 1585394817015
end_time = 1586451945230

OUTPUT_KEY_FEILD_MAPPING = {
    'Request Id': 'data.eventData.workflowId',
    'IVR ID': 'data.eventData.data.type',
    'IVR Language': 'data.eventData.data.language',
    'Phone Number': 'data.eventData.data.phone',
    'Location': 'data.eventData.data.state',
    'Pincode': 'data.eventData.data.providerTicketPincode',
    'Claimed by': 'data.eventData.data.providerTicketAgentName',
    'Name': 'data.eventData.data.providerTicketPatientName',
    'Age': 'data.eventData.data.providerTicketPatientAge',
    'Sex': 'data.eventData.data.providerTicketPatientGender',
    'Travel History': 'data.eventData.data.providerTicketForeignTravelHistory',
    'Contact History': 'data.eventData.data.providerTicketContact',
    'Current Status': 'data.eventData.data.providerTicketStatus',
    'Triage Results': 'data.eventData.data.providerTicketType',
    'Time of Action': 'data.time',
    'Doctor Notes': 'data.eventData.data.providerTicketDoctorNotes'
}


def today():
    return datetime.date.today()


def odisha_frh(start_time, end_time, file_name):
    foxtrot.generate_report("statesman",
                            [
                                {
                                    "operator": "equals",
                                    "value": "odisha",
                                    "field": "eventData.data.state"
                                },
                                {
                                    "operator": "equals",
                                    "value": "STATE_CHANGED",
                                    "field": "eventType"
                                },
                                {
                                    "operator": "equals",
                                    "value": "CALL_ATTENDED",
                                    "field": "eventData.newState"
                                },
                                {
                                    "operator": "equals",
                                    "value": "covid_visit_frh_first_response_hospital_",   #covid_home_quarantine
                                    "field": "eventData.update.providerTicketType"
                                }
                            ],
                            OUTPUT_KEY_FEILD_MAPPING,
                            file_name,
                            start_time,
                            end_time)

file_name = "/var/tmp/reports/odisha_frh_{}.csv".format(today())
odisha_frh(start_time, end_time, file_name)
