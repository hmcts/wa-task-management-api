package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest
@ActiveProfiles({"integration"})
public class DueDateConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    @Autowired
    private DueDateConfigurator dueDateConfigurator;

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDateAndTimesAreAvailable(
        String isReConfigurationRequest,
        String canConfigure) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDate, defaultDueDateTime, dueDate, dueDateTime),
                "WA",
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldNotCalculateDueDateWhenMultipleDueDateAndTimesAreAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDate, defaultDueDateTime, dueDate, dueDateTime), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDatesAreAvailable(
        String isReConfigurationRequest,
        String canConfigure) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, dueDate), "WA", Boolean.parseBoolean(isReConfigurationRequest));

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
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, dueDate), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDateTimesAreAvailable(
        String isReConfigurationRequest,
        String canConfigure
    ) {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDateTime, dueDateTime),
                "WA",
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @Test
    public void shouldNoCalculateDueDateWhenMultipleDueDateTimesAreAvailable() {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDateTime, dueDateTime), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotCalculateDueDateWhenDefaultDueDateWithoutTimeAndTimeAreAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate, defaultDueDateTime), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenDefaultDueDateWithoutTimeAndTimeAreAvailable(
        String isReConfigurationRequest,
        String canConfigure
    ) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDate, defaultDueDateTime),
                "WA",
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateTimeIsAvailable(
        String isReConfigurationRequest,
        String canConfigure
    ) {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDateTime), "WA", Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldNotCalculateDueDateWhenOnlyDefaultUnConfigurableDueDateTimeIsAvailable() {

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDateTime), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true",
        "true,true"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithTimeIsAvailable(
        String isReConfigurationRequest,
        String canConfigure
    ) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate), "WA", Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,T16:00",
        "true,true,T16:00"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithoutTimeIsAvailable(
        String isReConfigurationRequest,
        String canConfigure,
        String time
    ) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(defaultDueDate), "WA", Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + time))
                                   .build()));
    }

    @Test
    public void shouldNotReturnDueDateWhenOnlyDefaultUnconfigurableDueDateWithoutTimeIsAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDate),
                "WA",
                true
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @Test
    public void shouldNotReturnDueDateWhenNoDueDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(), "IA", false);

        Assertions.assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnDefaultDueDateWhenDueDatePropertiesAreNotAvailableAndJurisdictionIsWA() {
        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(), "WA", false);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldNotReturnDDueDateWhenDueDatePropertiesAreNotAvailableAndJurisdictionIsWAAndIsReconfiguration() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(List.of(), "WA", true);

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(0);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,T18:00",
        "true,false,T16:00",
        "true,true,T18:00"
    })
    public void shouldCalculateDateFromDueDateEvenWhenDueDateOriginPropertiesAreProvided(
        String isReConfigurationRequest,
        String canConfigure,
        String time
    ) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(defaultDueDate, dueDateOrigin, dueDateTime),
                "WA",
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + time))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,false,true,true,6,8,T18:00",
        "false,false,true,true,8,12,T18:00",
        "false,false,true,false,6,8,T18:00",
        "false,false,false,true,6,6,T18:00",
        "false,false,false,true,2,4,T18:00",
        "false,false,false,false,6,6,T18:00",
        "true,true,true,true,6,8,T18:00",
        "true,true,true,true,8,12,T18:00",
        "true,true,true,false,6,8,T18:00",
        "true,true,false,true,6,6,T18:00",
        "true,true,false,true,2,4,T18:00",
        "true,true,false,false,6,6,T18:00",
        "true,false,true,true,6,0,T16:00",
        "true,false,true,true,8,0,T16:00",
        "true,false,true,false,6,0,T16:00",
        "true,false,false,true,6,0,T16:00",
        "true,false,false,true,2,0,T16:00",
        "true,false,false,false,6,0,T16:00"
    })
    public void shouldCalculateDateWhenAllDueDateOriginPropertiesAreProvided(
        String isReConfigurationRequest,
        String canConfigure,
        String dueDateSkipNonWorkingDaysFlag,
        String dueDateMustBeWorkingDayFlag,
        String intervalDays,
        String expectedDays,
        String expectedTime) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        boolean canConfigurable = Boolean.parseBoolean(canConfigure);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue(intervalDays))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue(dueDateSkipNonWorkingDaysFlag))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(dueDateMustBeWorkingDayFlag))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dueDateConfigurator
            .configureDueDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                "WA",
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + expectedTime))
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
                "WA",
                false
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
