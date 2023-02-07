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

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;

@SpringBootTest
@ActiveProfiles({"integration"})
public class DueDateTypeConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);

    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 00, 00);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 03, 26, 18, 00, 00);
    @Autowired
    private DateTypeConfigurator dateTypeConfigurator;
    private String isReConfigurationRequest = "false";

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDateOriginsAreAvailable(
        String canConfigure,
        String initiationDueDateFound) {

        String firstDueDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondDueDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String thirdDueDate = GIVEN_DATE.plusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(firstDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(secondDueDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse thirdDueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(thirdDueDate + "T10:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateOrigin, dueDateOrigin, thirdDueDateOrigin),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(thirdDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,T20:00"
    })
    public void shouldCalculateDueDateWhenMultipleDueDateAndTimesAreAvailable(
        String canConfigure,
        String expectedTime) {

        String firstDueDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondDueDate = GIVEN_DATE.plusDays(9).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(firstDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("10:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(secondDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate, defaultDueDateTime, dueDate, dueDateTime), false,
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(secondDueDate + expectedTime))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(secondDueDate + expectedTime))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true, true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDatesAreAvailable(
        String canConfigure,
        String initiationDueDateFound) {

        String firstDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String secondDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(firstDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(secondDueDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate, dueDate),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(secondDueDate + "T18:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenMultipleDueDateTimesAreAvailable(
        String canConfigure,
        String initiationDueDateFound
    ) {


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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateTime, dueDateTime),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest));

        String defaultDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDueDate + "T18:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenDefaultDueDateWithoutTimeAndTimeAreAvailable(
        String canConfigure,
        String initiationDueDateFound
    ) {

        String givenDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(givenDueDate))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate, defaultDueDateTime),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(givenDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenOnlyDueDateTimeIsAvailable(
        String canConfigure,
        String initiationDueDateFound
    ) {

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateTime),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest)
            );
        String defaultDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithTimeIsAvailable(
        String canConfigure,
        String initiationDueDateFound
    ) {

        String givenDueDate = GIVEN_DATE.plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(givenDueDate + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest)
            );


        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(givenDueDate + "T19:00"))
                                   .build()));
    }

    @Test
    public void shouldNotReturnDueDateWhenNoDueDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(), false, true);

        Assertions.assertThat(configurationDmnEvaluationResponses).isEmpty();
    }

    @Test
    public void shouldReturnDefaultDueDateWhenNoDueDatePropertiesAreAvailableAndJurisdictionIsIA() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(), false, false);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldReturnDefaultDueDateWhenDueDatePropertiesAreNotAvailableAndInitiationDueDateNotFound() {

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(), false, false);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,true"
    })
    public void shouldCalculateDueDateWhenOnlyDueDateAndDueDateOriginBothProvided(
        String canConfigure,
        String initiationDueDateFound
    ) {
        String givenDueDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String givenDueDateOrigin = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(givenDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(givenDueDateOrigin + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(dueDate, dueDateOrigin),
                           Boolean.parseBoolean(initiationDueDateFound),
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(givenDueDate + "T16:00"))
                                   .build()));

    }

    @ParameterizedTest
    @CsvSource(value = {
        "true"
    })
    public void shouldCalculateDueDateWhenOnlyDueDateOriginIsProvided(
        String canConfigure
    ) {
        String givenDueDate = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(givenDueDate + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(dueDateOrigin), false, Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(givenDueDate + "T16:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(givenDueDate + "T16:00"))
                                   .build()));

    }

    @ParameterizedTest
    @CsvSource(value = {
        "true"
    })
    public void shouldCalculateDateFromDueDateEvenWhenDueDateOriginPropertiesAreProvided(String canConfigure) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate, dueDateOrigin, dueDateTime), false,
                           Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T18:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateTimeIsAvailable(String canConfigure) {

        String expectedDueDate = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateTime), false, Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,T16:00"
    })
    public void shouldCalculateDueDateWhenOnlyDefaultDueDateWithoutTimeIsAvailable(
        String canConfigure,
        String time
    ) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate), false, Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + time))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + time))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,Next,6,6,T18:00",
        "false,true,Next,8,8,T18:00",
        "false,true,No,6,6,T18:00",
        "false,false,Next,6,6,T18:00",
        "false,false,Next,2,2,T18:00",
        "false,false,No,6,6,T18:00",
        "false,false,Previous,2,2,T18:00"
    })
    public void shouldCalculateDateWhenAllDueDateOriginPropertiesAreProvidedAndNonWorkingDayNotConsidered(
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
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
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
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue(dueDateSkipNonWorkingDaysFlag))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                false,
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + expectedTime))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + expectedTime))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true,Next,6,8,T18:00",
        "false,true,next,8,12,T18:00",
        "false,true,no,6,8,T18:00",
        "false,false,Next,6,6,T18:00",
        "false,false,Next,2,4,T18:00",
        "false,false,no,6,6,T18:00",
        "false,false,Previous,2,1,T18:00"
    })
    public void shouldCalculateDateWhenAllDueDateOriginPropertiesAreProvided(
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
            .canReconfigure(CamundaValue.booleanValue(canConfigurable))
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                false,
                Boolean.parseBoolean(isReConfigurationRequest)
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + expectedTime))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin
                ),
                false,
                false
            );

        String expectedDueDate = GIVEN_DATE.plusDays(Integer.parseInt("8"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
                                   .build()));
    }

    @Test
    public void shouldNoCalculateDueDateWhenMultipleDueDateTimesAreAvailable() {

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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateTime, dueDateTime), false, true);

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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDate, defaultDueDateTime), false, true);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(0);
    }

    @Test
    public void shouldNotCalculateDueDateWhenOnlyDefaultUnConfigurableDueDateTimeIsAvailable() {

        ConfigurationDmnEvaluationResponse defaultDueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(defaultDueDateTime), false, true);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(0);
    }

    @Test
    public void shouldNotReturnDueDateWhenOnlyDefaultUnconfigurableDueDateWithoutTimeIsAvailable() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(defaultDueDate),
                false,
                true
            );

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(0);
    }

    @Test
    public void shouldNotReturnDDueDateWhenDueDatePropertiesAreNotAvailableAndJurisdictionIsWAAndIsReconfiguration() {
        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(List.of(), false, true);

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(0);
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTBackword() {
        String localDateTime = BST_DATE_BACKWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go back an hour at 2:00am
        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T01:30"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("02:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                false, false
            );

        String expectedDueDate = "2022-10-30T02:30";

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate))
                                   .build()));
    }

    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDueDateNonBSTForward() {
        String localDateTime = BST_DATE_FORWARD.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        //Clocks go forward an hour at 1:00am
        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T00:30"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("4"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("01:30"))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                false, false
            );

        String expectedDueDate = "2023-03-30T01:30";

        Assertions.assertThat(configurationDmnEvaluationResponses).hasSize(2)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(expectedDueDate))
                                   .build()));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false,true, true",
        "true,true, false"
    })
    public void shouldCalculateDueDateWhenCalculatedDatesAreAvailable(
        String isReConfigurationRequest,
        String canConfigure,
        String initiationDueDateFound) {

        String dueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse defaultDueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(dueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("dueDate,priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(Boolean.parseBoolean(canConfigure)))
            .build();

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = dateTypeConfigurator
            .configureDates(List.of(defaultDueDate, calculatedDate),
                            Boolean.parseBoolean(initiationDueDateFound),
                            Boolean.parseBoolean(isReConfigurationRequest));

        Assertions.assertThat(configurationDmnEvaluationResponses)
            .filteredOn(r -> r.getName().getValue().equals("dueDate"))
            .hasSize(1)
            .isEqualTo(List.of(ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(dueDate + "T16:00"))
                                   .build()));
    }

}
