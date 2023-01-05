package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.BankHolidaysApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.SnakeCaseFeignConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.BankHolidays;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores all public holidays for england and wales retrieved from Gov uk API: https://www.gov.uk/bank-holidays/england-and-wales.json .
 */
@Slf4j
@Component
@Import(SnakeCaseFeignConfiguration.class)
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class PublicHolidaysCollection {

    public final Decoder feignDecoder;
    public final Encoder feignEncoder;

    public PublicHolidaysCollection(Decoder feignDecoder, Encoder feignEncoder) {
        this.feignDecoder = feignDecoder;
        this.feignEncoder = feignEncoder;
    }

    public Set<LocalDate> getPublicHolidays(List<String> uris) {
        List<BankHolidays.EventDate> events = new ArrayList<>();
        BankHolidays allPublicHolidays = BankHolidays.builder().events(events).build();
        if (uris != null) {
            for (String uri : uris) {
                BankHolidays publicHolidays = getPublicHolidays(uri);
                processCalendar(publicHolidays, allPublicHolidays);
            }
        }

        return allPublicHolidays.getEvents().stream()
            .map(item -> LocalDate.parse(item.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .collect(Collectors.toSet());
    }

    @Cacheable(value = "public_holidays_uri_cache", key = "#uri", sync = true)
    public BankHolidays getPublicHolidays(String uri) {
        BankHolidaysApi bankHolidaysApi = bankHolidaysApi(uri);
        return bankHolidaysApi.retrieveAll();
    }

    private void processCalendar(BankHolidays publicHolidays, BankHolidays allPublicHolidays) {
        for (BankHolidays.EventDate eventDate : publicHolidays.getEvents()) {
            if (eventDate.isWorkingDay()) {
                if (allPublicHolidays.getEvents().contains(eventDate)) {
                    allPublicHolidays.getEvents().remove(eventDate);
                }
            } else {
                allPublicHolidays.getEvents().add(eventDate);
            }
        }
    }

    private BankHolidaysApi bankHolidaysApi(String uri) {
        return Feign.builder()
            .decoder(feignDecoder)
            .encoder(feignEncoder)
            .target(BankHolidaysApi.class, uri);
    }

}
