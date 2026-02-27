package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.wataskmanagementapi.Application;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.BankHolidays;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.CalendarResourceInvalidException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.CalendarResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.CalendarTestSupport.CALENDAR_URI;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.CalendarTestSupport.INVALID_CALENDAR_URI;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.CalendarTestSupport.OVERRIDE_CALENDAR_URI;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.CalendarTestSupport.notFoundUri;

@IntegrationTest
class PublicHolidaysCollectionTest {

    @Autowired
    private PublicHolidaysCollection publicHolidaysCollection;

    @Autowired
    private PublicHolidayService publicHolidayService;

    @Test
    public void should_not_return_empty_bank_holidays() {
        Set<LocalDate> response = publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI));
        assertThat(response).isNotEmpty();
    }

    @Test
    public void should_call_external_api_only_once() {
        BankHolidays resultFromApi = publicHolidayService.getPublicHolidays(CALENDAR_URI);
        BankHolidays resultFromCache = publicHolidayService.getPublicHolidays(CALENDAR_URI);
        BankHolidays resultFromCacheAgain = publicHolidayService.getPublicHolidays(CALENDAR_URI);

        assertThat(resultFromApi).isSameAs(resultFromCache).isSameAs(resultFromCacheAgain);
    }

    @Test
    public void second_calendar_should_override_main_calendar() {
        List<String> oneUri = List.of(CALENDAR_URI);
        Set<LocalDate> oneCalendarResult = publicHolidaysCollection.getPublicHolidays(oneUri);
        assertThat(oneCalendarResult.contains(LocalDate.of(2026, 05, 25))).isTrue();
        assertThat(oneCalendarResult.contains(LocalDate.of(2026, 12, 25))).isTrue();

        List<String> twoUris = List.of(
            CALENDAR_URI,
            OVERRIDE_CALENDAR_URI
        );
        Set<LocalDate> twoCalendarResult = publicHolidaysCollection.getPublicHolidays(twoUris);
        assertThat(twoCalendarResult.contains(LocalDate.of(2026, 05, 25))).isFalse();
        assertThat(twoCalendarResult.contains(LocalDate.of(2026, 12, 25))).isTrue();
    }

    @Test
    public void should_return_empty_list_with_null() {
        List<String> nullList = null;
        Set<LocalDate> oneCalendarResult = publicHolidaysCollection.getPublicHolidays(nullList);
        assertThat(oneCalendarResult.size()).isEqualTo(0);
    }

    @Test
    public void should_throw_calendar_resource_not_found_exception() {
        String wrongUri = notFoundUri();
        assertThatThrownBy(() -> publicHolidaysCollection.getPublicHolidays(List.of(wrongUri)))
            .isInstanceOf(CalendarResourceNotFoundException.class)
            .hasMessage("Could not find calendar resource " + wrongUri);
    }

    @Test
    public void should_throw_resource_invalid_exception() {
        assertThatThrownBy(() -> publicHolidaysCollection.getPublicHolidays(List.of(INVALID_CALENDAR_URI)))
            .isInstanceOf(CalendarResourceInvalidException.class)
            .hasMessage("Could not read calendar resource " + INVALID_CALENDAR_URI);
    }

    @Test
    public void should_change_after_cache_expiry_external_api() {
        BankHolidays resultFromApi = publicHolidayService.getPublicHolidays(CALENDAR_URI);

        PublicHolidaysCollectionTest.TestConfiguration.fakeTicker.advance(10, TimeUnit.HOURS);

        BankHolidays resultFromCache = publicHolidayService.getPublicHolidays(CALENDAR_URI);

        assertThat(resultFromApi).isSameAs(resultFromCache);

        PublicHolidaysCollectionTest.TestConfiguration.fakeTicker.advance(25, TimeUnit.HOURS);

        BankHolidays resultFromRenewedCache = publicHolidayService.getPublicHolidays(CALENDAR_URI);

        assertThat(resultFromApi).isSameAs(resultFromCache).isNotSameAs(resultFromRenewedCache);
    }

    @Configuration
    @Import(Application.class)
    public static class TestConfiguration {

        static FakeTicker fakeTicker = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return fakeTicker::read;
        }

    }
}
