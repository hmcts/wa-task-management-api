package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import uk.gov.hmcts.reform.wataskmanagementapi.config.SnakeCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.BankHolidays;

import java.net.URI;

@FeignClient(
    name = "bank-holiday-api",
    url = "https://this-is-a-placeholder.com",
    configuration = SnakeCaseFeignConfiguration.class
)
public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll(URI baseUrl);
}
