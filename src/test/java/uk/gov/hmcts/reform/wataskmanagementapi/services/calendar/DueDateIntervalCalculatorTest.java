package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NO;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculatorTest.DUE_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class DueDateIntervalCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private DueDateIntervalCalculator dueDateIntervalCalculator;

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(3).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.plusDays(3).format(DATE_TIME_FORMATTER) + "T18:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWithoutDueDate() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T20:00"),
            new ConfigurableScenario(false, GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T20:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(1).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.plusDays(1).format(DATE_TIME_FORMATTER) + "T18:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesSkipNonWorkingDaysFalse() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(6).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.plusDays(6).format(DATE_TIME_FORMATTER) + "T18:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0AndGivenHolidays() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(7).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.plusDays(7).format(DATE_TIME_FORMATTER) + "T18:00")
        );
    }

    @BeforeEach
    public void before() {
        dueDateIntervalCalculator = new DueDateIntervalCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
        "true", "false"
    })
    void shouldCalculateWhenDefaultValueProvided(boolean configurable) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateIntervalCalculator
                                                           .calculateDate(
                                                               List.of(
                                                                   dueDateIntervalDays,
                                                                   dueDateNonWorkingCalendar,
                                                                   dueDateMustBeWorkingDay,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays,
                                                                   dueDateOrigin,
                                                                   dueDateTime
                                                               ),
                                                               DUE_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(0).format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenIntervalIsGreaterThan0"})
    void shouldCalculateWhenIntervalIsGreaterThan0(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE_TYPE,
                scenario.configurable
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(dueDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenIntervalIsGreaterThan0AndGivenHolidays"})
    void shouldCalculateWhenIntervalIsGreaterThan0AndGivenHolidays(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE_TYPE,
                scenario.configurable
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dueDateValue);
        assertThat(resultDate).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesSkipNonWorkingDaysFalse"})
    void shouldCalculateWhenSkipNonWorkingDaysFalse(ConfigurableScenario scenario) {
        when(publicHolidaysCollection.getPublicHolidays(List.of(CALENDAR_URI)))
            .thenReturn(Set.of(LocalDate.of(2022, 10, 18)));

        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE_TYPE,
                scenario.configurable
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(dueDateValue)).isEqualTo(scenario.expectedDate);
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .build();

        String dateValue = dueDateIntervalCalculator.calculateDate(
            List.of(
                dueDateIntervalDays,
                dueDateNonWorkingCalendar,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays,
                dueDateOrigin,
                dueDateTime
            ),
            DUE_DATE_TYPE,
            false
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedDueDate = GIVEN_DATE.plusDays(4)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessFalse(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE_TYPE,
                scenario.configurable
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(dueDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWithoutDueDate"})
    void shouldCalculateWhenWithoutDueDateTime(ConfigurableScenario scenario) {
        boolean isConfigurable = scenario.configurable;
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(scenario.configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NO))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateIntervalCalculator
                                                           .calculateDate(
                                                               List.of(
                                                                   dueDateIntervalDays,
                                                                   dueDateNonWorkingCalendar,
                                                                   dueDateMustBeWorkingDay,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays,
                                                                   dueDateOrigin
                                                               ),
                                                               DUE_DATE_TYPE,
                                                               scenario.configurable
                                                           ).getValue().getValue());

        assertThat(resultDate).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00",
        "false, T20:00"
    })
    void shouldCalculateWhenOnlyDueDateOriginProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();


        LocalDateTime resultDate = LocalDateTime
            .parse(dueDateIntervalCalculator.calculateDate(
                List.of(dueDateOrigin),
                DUE_DATE_TYPE,
                configurable
            ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T18:00",
        "false, T18:00"
    })
    void shouldCalculateWhenOnlyDueDateOriginAndTimeProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateIntervalCalculator.calculateDate(
            List.of(dueDateOrigin, dueDateTime),
            DUE_DATE_TYPE,
            false
        ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_due_date_origin_and_configurable_due_date(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDate);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_due_date_origin_and_due_date(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0).format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDate);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable))
            .isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_only_due_date_time(boolean configurable) {

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_only_contains_due_date_origin_but_not_due_date(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_only_contains_due_date_but_not_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable))
            .isTrue();
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

