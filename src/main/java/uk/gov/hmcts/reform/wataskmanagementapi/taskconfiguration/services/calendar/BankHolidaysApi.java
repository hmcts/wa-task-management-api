package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import feign.RequestLine;

public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll();
}
