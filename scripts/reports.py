import datetime

import foxtrot

start_time = 1585394817015
end_time = 1586258805471

output_key_names = [
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


def today():
    return datetime.date.today()


def punjab_frh(start_time, end_time, file_handler):
    foxtrot.execute_foxtrot_query_paginated("statesman",
                                            [
                                                       {
                                                           "operator": "equals",
                                                           "value": "punjab",
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
                                                       }

                                                   ],
                                            [
                                                       'data.eventData.workflowId',
                                                       'data.eventData.data.type',
                                                       'data.eventData.data.language',
                                                       'data.eventData.data.phone',
                                                       'data.eventData.data.state',
                                                       'data.eventData.data.providerTicketPincode',
                                                       'data.eventData.data.providerTicketAgentName',
                                                       'data.eventData.data.providerTicketPatientName',
                                                       'data.eventData.data.providerTicketPatientAge',
                                                       'data.eventData.data.providerTicketPatientGender',
                                                       'data.eventData.update.providerTicketForeignTravelHistory',
                                                       'data.eventData.data.contact',
                                                       'data.eventData.oldState',
                                                       'none',
                                                       'data.eventData.update.providerTicketStatus',
                                                       'data.eventData.update.providerTicketActionRecommended',
                                                       'data.time',
                                                       'data.eventData.data.providerTicketDoctorNotes',
                                                       'none'
                                                   ],
                                            output_key_names,
                                            file_handler,
                                            start_time,
                                            end_time)


FILEPATH = "/var/tmp/reports/punjab_frh_{}.csv".format(today())
output_file = open(FILEPATH, "w")
punjab_frh(start_time, end_time, output_file)
output_file.close()
