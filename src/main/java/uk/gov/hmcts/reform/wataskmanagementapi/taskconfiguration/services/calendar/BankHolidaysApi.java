package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.URI;

//@FeignClient(name = "bank-holidays-api")
public interface BankHolidaysApi {

    @RequestLine("GET")
    BankHolidays retrieveAll(URI uri);
}
