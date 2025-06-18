package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.Application;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_NON_WORKING_CALENDAR;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CalendarCacheManagerTest.TestConfiguration.class)
@ActiveProfiles({"integration"})
public class CalendarCacheManagerTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;

    @MockitoSpyBean
    private PublicHolidayService publicHolidayService;

    @DisplayName("(Access calendars successfully and retrieve results which are also cached)")
    @Test
    public void shouldCacheThePublicHolidayCalendarResponse() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("6"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin
                ),
                false,
                false,
                new HashMap<>()
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt("8"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
                    .build()
            ));
        verify(publicHolidayService, times(1)).getPublicHolidays(DEFAULT_NON_WORKING_CALENDAR);
    }

    @DisplayName("(Access calendars successfully if cached information is not available)")
    @Test
    public void shouldCallHolidayServiceIfCacheExpires() {
        TestConfiguration.fakeTicker.advance(25, TimeUnit.HOURS);

        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("6"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin
                ),
                false,
                false,
                new HashMap<>()
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt("8"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("dueDate"))
                    .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
                    .canReconfigure(CamundaValue.booleanValue(false))
                    .build(),
                ConfigurationDmnEvaluationResponse.builder()
                    .name(CamundaValue.stringValue("priorityDate"))
                    .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
                    .build()
            ));

        verify(publicHolidayService, times(1)).getPublicHolidays(DEFAULT_NON_WORKING_CALENDAR);
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
