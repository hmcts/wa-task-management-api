package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.PublicHolidaysCollection;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
}
