package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PriorityDateCalculatorTest.PRIORITY_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class PriorityDateIntervalReCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private PriorityDateIntervalReCalculator priorityDateIntervalReCalculator;

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0() {
        return Stream.of(
            new ConfigurableScenario(
                true,
                GIVEN_DATE.plusDays(3).format(DATE_TIME_FORMATTER) + "T18:00"
            ),
            new ConfigurableScenario(
                false,
                GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T20:00"
            )
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWithoutPriorityDate() {
        return Stream.of(
            new ConfigurableScenario(
                true,
                GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T20:00"
            ),
            new ConfigurableScenario(
                false,
                GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T20:00"
            )
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse() {
        return Stream.of(
            new ConfigurableScenario(
                true,
                GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T18:00"
            ),
            new ConfigurableScenario(
                false,
                GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T20:00"
            )
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesSkipNonWorkingDaysFalse() {
        return Stream.of(
            new ConfigurableScenario(
                true,
                GIVEN_DATE.plusDays(6).format(DATE_TIME_FORMATTER) + "T18:00"
            ),
            new ConfigurableScenario(
                false,
                GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T20:00"
            )
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0AndGivenHolidays() {
        return Stream.of(
            new ConfigurableScenario(
                true,
                GIVEN_DATE.plusDays(7).format(DATE_TIME_FORMATTER) + "T18:00"
            ),
            new ConfigurableScenario(
                false,
                GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T20:00"
            )
        );
    }

    @BeforeEach
    public void before() {
        priorityDateIntervalReCalculator = new PriorityDateIntervalReCalculator(new WorkingDayIndicator(
            publicHolidaysCollection));

        Set<LocalDate> localDates = Set.of(
            LocalDate.of(2022, 1, 3),
            LocalDate.of(2022, 4, 15),
            LocalDate.of(2022, 4, 18),
            LocalDate.of(2022, 5, 2),
            LocalDate.of(2022, 6, 2),
            LocalDate.of(2022, 6, 3),
            LocalDate.of(2022, 8, 29),
            LocalDate.of(2022, 9, 19),
            LocalDate.of(2022, 12, 26),
            LocalDate.of(2022, 12, 27)
        );

        lenient().when(publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI))).thenReturn(localDates);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T18:00",
        "false, T20:00"
    })
    void shouldCalculateWhenDefaultValueProvided(String configurable, String time) {
        boolean isConfigurable = Boolean.parseBoolean(configurable);
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateIntervalReCalculator
                                                           .calculateDate(
                                                               PRIORITY_DATE_TYPE, List.of(
                                                                   priorityDateIntervalDays,
                                                                   priorityDateNonWorkingCalendar,
                                                                   priorityDateMustBeWorkingDay,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateSkipNonWorkingDays,
                                                                   priorityDateOrigin,
                                                                   priorityDateTime
                                                               )
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedPriorityDate + time);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenIntervalIsGreaterThan0"})
    void shouldCalculateWhenIntervalIsGreaterThan0(ConfigurableScenario scenario) {
        boolean isConfigurable = scenario.configurable;

        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        String priorityDateValue = priorityDateIntervalReCalculator
            .calculateDate(
                PRIORITY_DATE_TYPE, List.of(
                    priorityDateIntervalDays,
                    priorityDateNonWorkingCalendar,
                    priorityDateMustBeWorkingDay,
                    priorityDateNonWorkingDaysOfWeek,
                    priorityDateSkipNonWorkingDays,
                    priorityDateOrigin,
                    priorityDateTime
                )
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(priorityDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenIntervalIsGreaterThan0AndGivenHolidays"})
    void shouldCalculateWhenIntervalIsGreaterThan0AndGivenHolidays(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String priorityDateValue = priorityDateIntervalReCalculator
            .calculateDate(
                PRIORITY_DATE_TYPE, List.of(
                    priorityDateIntervalDays,
                    priorityDateNonWorkingCalendar,
                    priorityDateMustBeWorkingDay,
                    priorityDateNonWorkingDaysOfWeek,
                    priorityDateSkipNonWorkingDays,
                    priorityDateOrigin,
                    priorityDateTime
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(priorityDateValue);
        assertThat(resultDate).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesSkipNonWorkingDaysFalse"})
    void shouldCalculateWhenSkipNonWorkingDaysFalse(ConfigurableScenario scenario) {
        when(publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI)))
            .thenReturn(Set.of(LocalDate.of(2022, 10, 18)));

        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String priorityDateValue = priorityDateIntervalReCalculator
            .calculateDate(
                PRIORITY_DATE_TYPE, List.of(
                    priorityDateIntervalDays,
                    priorityDateNonWorkingCalendar,
                    priorityDateMustBeWorkingDay,
                    priorityDateNonWorkingDaysOfWeek,
                    priorityDateSkipNonWorkingDays,
                    priorityDateOrigin,
                    priorityDateTime
                )
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(priorityDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessFalse(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String priorityDateValue = priorityDateIntervalReCalculator
            .calculateDate(
                PRIORITY_DATE_TYPE, List.of(
                    priorityDateIntervalDays,
                    priorityDateNonWorkingCalendar,
                    priorityDateMustBeWorkingDay,
                    priorityDateNonWorkingDaysOfWeek,
                    priorityDateSkipNonWorkingDays,
                    priorityDateOrigin,
                    priorityDateTime
                )
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(priorityDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWithoutPriorityDate"})
    void shouldCalculateWhenWithoutPriorityDateTime(ConfigurableScenario scenario) {
        boolean isConfigurable = scenario.configurable;
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateIntervalReCalculator
                                                           .calculateDate(
                                                               PRIORITY_DATE_TYPE, List.of(
                                                                   priorityDateIntervalDays,
                                                                   priorityDateNonWorkingCalendar,
                                                                   priorityDateMustBeWorkingDay,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateSkipNonWorkingDays,
                                                                   priorityDateOrigin
                                                               )
                                                           ).getValue().getValue());

        assertThat(resultDate).isEqualTo(scenario.expectedDate);
    }

    @Test
    void shouldCalculateWhenOnlyPriorityDateOriginProvided() {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        LocalDateTime resultDate = LocalDateTime
            .parse(priorityDateIntervalReCalculator.calculateDate(
                PRIORITY_DATE_TYPE, List.of(priorityDateOrigin)
            ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T20:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true, T18:00",
        "false, T20:00"
    })
    void shouldCalculateWhenOnlyPriorityDateOriginAndTimeProvided(String configurable, String time) {
        boolean isConfigurable = Boolean.parseBoolean(configurable);
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        String priorityDateValue = priorityDateIntervalReCalculator.calculateDate(PRIORITY_DATE_TYPE, List.of(
                priorityDateOrigin,
                priorityDateTime
            ))
            .getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(priorityDateValue);
        String expectedPriorityDate = GIVEN_DATE.format(DATE_TIME_FORMATTER);
        assertThat(resultDate).isEqualTo(expectedPriorityDate + time);
    }

    @Test
    void should_not_supports_when_responses_contains_priority_date_origin_and_configurable_priority_date() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin, priorityDate);

        assertThat(priorityDateIntervalReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_priority_date_origin_and_un_configurable_priority_date() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin, priorityDate);

        assertThat(priorityDateIntervalReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_not_supports_when_responses_contains_only_priority_date_time() {

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime);

        assertThat(priorityDateIntervalReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_priority_date_origin_but_not_priority_date() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin, priorityDateTime);

        assertThat(priorityDateIntervalReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_not_supports_responses_only_containing_priority_date_origin_with_can_configure_false_but_not_origin() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin, priorityDateTime);

        assertThat(priorityDateIntervalReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    static class ConfigurableScenario {
        boolean configurable;
        String expectedDate;

        public ConfigurableScenario(boolean configurable, String expectedDate) {
            this.configurable = configurable;
            this.expectedDate = expectedDate;
        }
    }
}
