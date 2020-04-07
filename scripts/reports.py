import datetime

import foxtrot

start_time = 1585394817015
end_time = 1586258805471


def today():
    return datetime.date.today()


def punjab_frh(start_time, end_time, file_name):
    foxtrot.generate_report("statesman",
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
                                'none',
                                'none',
                                'data.eventData.data.type',
                                'data.eventData.data.language',
                                'data.eventData.data.phone',
                                'data.eventData.data.state',
                                'data.eventData.data.providerTicketPincode',
                                'data.eventData.data.providerTicketAgentName',
                                'data.eventData.data.providerTicketPatientName',
                                'data.eventData.data.providerTicketPatientAge',
                                'data.eventData.data.providerTicketPatientGender',
                                'data.eventData.data.providerTicketForeignTravelHistory',
                                'data.eventData.data.providerTicketContact',
                                'none',
                                'none',
                                'data.eventData.data.providerTicketStatus',
                                'data.eventData.data.providerTicketType',
                                'data.time',
                                'data.eventData.data.providerTicketDoctorNotes',
                                'none'
                            ],
                            file_name,
                            start_time,
                            end_time)


file_name = "/var/tmp/reports/punjab_frh_{}.csv".format(today())
punjab_frh(start_time, end_time, file_name)
