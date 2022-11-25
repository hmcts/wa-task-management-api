package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.BankHolidays;

import java.time.LocalDate;
import java.util.List;
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
        Set<LocalDate> response = publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI));
        assertThat(response).isNotEmpty();
    }

    @Test
    public void should_call_external_api_only_once() {
        BankHolidays resultFromApi = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);
        BankHolidays resultFromCache = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);
        BankHolidays resultFromCacheAgain = publicHolidaysCollection.getPublicHolidays(CALENDAR_URI);

        assertThat(resultFromApi).isSameAs(resultFromCache).isSameAs(resultFromCacheAgain);
    }

    @Test
    public void second_calendar_should_override_main_calendar() {
        List<String> oneUri = List.of(CALENDAR_URI);
        Set<LocalDate> oneCalendarResult = publicHolidaysCollection.getPublicHolidays(oneUri);
        assertThat(oneCalendarResult.contains(LocalDate.of(2022, 12, 26))).isTrue();
        assertThat(oneCalendarResult.contains(LocalDate.of(2022, 12, 27))).isTrue();

        List<String> twoUris = List.of(CALENDAR_URI, "https://raw.githubusercontent.com/hmcts/wa-task-management-api/RWA-1768-calendar-for-test/src/test/resources/override-working-day-calendar.json");
        Set<LocalDate> twoCalendarResult = publicHolidaysCollection.getPublicHolidays(twoUris);
        assertThat(twoCalendarResult.contains(LocalDate.of(2022, 12, 26))).isFalse();
        assertThat(twoCalendarResult.contains(LocalDate.of(2022, 12, 27))).isTrue();

    }
}
