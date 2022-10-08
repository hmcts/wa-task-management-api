package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores all public holidays retrieved from Gov uk API: https://www.gov.uk/bank-holidays.json .
 */
@Component
public class PublicHolidaysCollection {


    private Set<LocalDate> cachedPublicHolidays;

    private Set<LocalDate> retrieveAllPublicHolidays(String uri) {
        RestTemplate restTemplate = new RestTemplate();
        BankHolidays value = restTemplate.getForObject(URI.create(uri), BankHolidays.class);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(BankHolidays.Division.EventDate.FORMAT);

        return value.englandAndWales.events.stream()
            .map(item -> LocalDate.parse(item.date, formatter))
            .collect(Collectors.toSet());
    }

    public Set<LocalDate> getPublicHolidays(String uri) {
        if (cachedPublicHolidays == null) {
            cachedPublicHolidays = retrieveAllPublicHolidays(uri);
        }
        return cachedPublicHolidays;
    }
}
