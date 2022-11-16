package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions.CalendarResourceInvalidException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions.CalendarResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.PublicHolidaysCollection;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"integration"})
class PublicHolidaysCollectionTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";

    @Autowired
    private PublicHolidaysCollection publicHolidaysCollection;

    @Test
    public void should_not_return_empty_bank_holidays() {
        Set<LocalDate> response = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);
        assertThat(response).isNotEmpty();
    }

    @Test
    public void should_call_external_api_only_once() {
        Set<LocalDate> resultFromApi = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);
        Set<LocalDate> resultFromCache = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);
        Set<LocalDate> resultFromCacheAgain = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);

        assertThat(resultFromApi).isSameAs(resultFromCache).isSameAs(resultFromCacheAgain);
    }

    @Test
    public void should_throw_calendar_resource_not_found_exception() {
        String wrongUri = "https://www.gov.uk/bank-holidays/not-a-calendar.json";
        assertThatThrownBy(() -> publicHolidaysCollection.getPublicHolidays(wrongUri))
            .isInstanceOf(CalendarResourceNotFoundException.class)
            .hasMessage("Could not find calendar resource " + wrongUri);
    }

    @Test
    public void should_throw_resource_invalid_exception() {
        String uri = "https://raw.githubusercontent.com/hmcts/wa-task-management-api/aab9ae68c9424071d9d49235a8b8f3230b1f89a2/src/integrationTest/resources/calendars/invalid-calendar.json";
        assertThatThrownBy(() -> publicHolidaysCollection.getPublicHolidays(uri))
            .isInstanceOf(CalendarResourceInvalidException.class)
            .hasMessage("Could not find calendar resource " + uri);
    }


}
