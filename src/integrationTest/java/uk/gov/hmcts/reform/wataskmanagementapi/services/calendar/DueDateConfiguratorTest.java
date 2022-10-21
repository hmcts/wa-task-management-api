package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DueDateConfigurator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest
@ActiveProfiles({"integration"})
public class DueDateConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    @Autowired
    private DueDateConfigurator dueDateConfigurator;

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDateAndTimesAreAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, defaultDueDateTime, dueDate, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDatesAreAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, dueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenMultipleDueDateTimesAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDateTime, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenDefaultDueDateWithoutTimeAndTimeAreAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, defaultDueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateTimeIsAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }


    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithTimeIsAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithoutTimeIsAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldNotReturnDueDateWhenNoDueDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(), "IA");

        Assertions.assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnDefaultDueDateWhenDueDatePropertiesAreNotAvailableAndJurisdictionIsWA() {
        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDateFromDueDateEvenWhenDueDateOriginPropertiesAreProvided() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, dueDateOrigin, dueDateTime), "WA");

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true,6,8",
        "true,true,8,12",
        "true,false,6,8",
        "false,true,6,6",
        "false,true,2,4",
        "false,false,6,6"
    })
    public void shouldCalculateDateWhenAllDueDateOriginPropertiesAreProvided(
        String dueDateSkipNonWorkingDaysFlag,
        String dueDateMustBeWorkingDayFlag,
        String intervalDays,
        String expectedDays) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue(intervalDays))
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
            .value(CamundaValue.stringValue(dueDateSkipNonWorkingDaysFlag))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(dueDateMustBeWorkingDayFlag))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                "WA"
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldCalculateDateWhenAllDueDateOriginPropertiesAreProvidedWithoutDueDateTime() {
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin
                ),
                "WA"
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt("8"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }
}
