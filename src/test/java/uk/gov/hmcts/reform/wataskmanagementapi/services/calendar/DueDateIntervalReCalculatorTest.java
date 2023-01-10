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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@ExtendWith(MockitoExtension.class)
class DueDateIntervalReCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private DueDateIntervalReCalculator dueDateIntervalCalculator;

    @BeforeEach
    public void before() {
        dueDateIntervalCalculator = new DueDateIntervalReCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
        "false, T16:00"
    })
    void shouldCalculateWhenDefaultValueProvided(String configurable, String time) {
        boolean isConfigurable = Boolean.parseBoolean(configurable);
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE
            ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenIntervalIsGreaterThan0"})
    void shouldCalculateWhenIntervalIsGreaterThan0(ConfigurableScenario scenario) {
        boolean isConfigurable = scenario.configurable;

        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin, dueDateTime
                ),
                DUE_DATE
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
            .canReconfigure(CamundaValue.booleanValue(true))
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
            .value(CamundaValue.stringValue("true"))
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
                DUE_DATE
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
            .canReconfigure(CamundaValue.booleanValue(true))
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
            .value(CamundaValue.stringValue("true"))
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
                DUE_DATE
            ).getValue().getValue();

        assertThat(LocalDateTime.parse(dueDateValue)).isEqualTo(scenario.expectedDate);
    }

    @ParameterizedTest
    @MethodSource({"getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessFalse(ConfigurableScenario scenario) {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
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
            .value(CamundaValue.stringValue("false"))
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
                DUE_DATE
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
            .canReconfigure(CamundaValue.booleanValue(true))
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
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateIntervalCalculator
            .calculateDate(
                List.of(dueDateIntervalDays, dueDateNonWorkingCalendar, dueDateMustBeWorkingDay,
                        dueDateNonWorkingDaysOfWeek, dueDateSkipNonWorkingDays, dueDateOrigin
                ),
                DUE_DATE
            ).getValue().getValue());

        assertThat(resultDate).isEqualTo(scenario.expectedDate);
    }

    @Test
    void shouldCalculateWhenOnlyDueDateOriginProvided() {
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        LocalDateTime resultDate = LocalDateTime
            .parse(dueDateIntervalCalculator.calculateDate(List.of(dueDateOrigin), DUE_DATE).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        assertThat(resultDate).isEqualTo(expectedDueDate + "T16:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true, T18:00",
        "false, T16:00"
    })
    void shouldCalculateWhenOnlyDueDateOriginAndTimeProvided(String configurable, String time) {
        boolean isConfigurable = Boolean.parseBoolean(configurable);
        String localDateTime = GIVEN_DATE.format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        String dueDateValue = dueDateIntervalCalculator.calculateDate(List.of(dueDateOrigin, dueDateTime), DUE_DATE)
            .getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dueDateValue);
        String expectedDueDate = GIVEN_DATE.format(DATE_TIME_FORMATTER);
        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @Test
    void should_not_supports_when_responses_contains_due_date_origin_and_configurable_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDate);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_due_date_origin_and_un_configurable_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDate);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isTrue();
    }

    @Test
    void should_not_supports_when_responses_contains_only_due_date_time() {

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_due_date_origin_but_not_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isTrue();
    }

    @Test
    void should_not_supports_when_responses_only_contains_due_date_origin_with_can_configure_false_but_not_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DATE_TIME_FORMATTER);

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDateTime);

        assertThat(dueDateIntervalCalculator.supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isFalse();
    }


    static class ConfigurableScenario {
        boolean configurable;
        String expectedDate;

        public ConfigurableScenario(boolean configurable, String expectedDate) {
            this.configurable = configurable;
            this.expectedDate = expectedDate;
        }
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(3).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T16:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWithoutDueDate() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T16:00"),
            new ConfigurableScenario(false, GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T16:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenSkipNonWorkingDaysAndMustBeBusinessFalse() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(5).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T16:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesSkipNonWorkingDaysFalse() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(6).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T16:00")
        );
    }

    private static Stream<ConfigurableScenario> getConfigurablesWhenIntervalIsGreaterThan0AndGivenHolidays() {
        return Stream.of(
            new ConfigurableScenario(true, GIVEN_DATE.plusDays(7).format(DATE_TIME_FORMATTER) + "T18:00"),
            new ConfigurableScenario(false, GIVEN_DATE.format(DATE_TIME_FORMATTER) + "T16:00")
        );
    }
}

