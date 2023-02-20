package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import feign.RequestLine;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.BankHolidays;

public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll();
}
