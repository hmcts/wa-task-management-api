package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import feign.RequestLine;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.BankHolidays;

public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll();
}
