package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PriorityDateCalculatorTest.PRIORITY_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class PriorityDateOriginEarliestCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private PriorityDateOriginEarliestCalculator priorityDateOriginEarliestCalculator;

    @BeforeEach
    public void before() {
        priorityDateOriginEarliestCalculator
            = new PriorityDateOriginEarliestCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
    void should_not_supports_when_responses_contains_priority_date(boolean configurable) {

        var priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var priorityDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .build();

        var priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            priorityDateOrigin,
            priorityDateIntervalDays,
            priorityDateNonWorkingCalendar,
            priorityDateNonWorkingDaysOfWeek,
            priorityDateSkipNonWorkingDays,
            priorityDateMustBeWorkingDay,
            priorityDateTime
        );

        assertThat(priorityDateOriginEarliestCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            configurable
        )).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_priority_date_origin(boolean configurable) {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin);

        assertThat(priorityDateOriginEarliestCalculator.supports(evaluationResponses, PRIORITY_DATE_TYPE, configurable))
            .isFalse();
    }


    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_supports_when_responses_only_contains_priority_date_origin_earliest(boolean configurable) {
        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(priorityDateOriginEarliest, priorityDateTime);

        assertThat(priorityDateOriginEarliestCalculator.supports(evaluationResponses, PRIORITY_DATE_TYPE, configurable))
            .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateNotNullWhenOriginDateValueProvided(boolean configurable) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        var configurationDmnEvaluationResponse = priorityDateOriginEarliestCalculator
            .calculateDate(
                readPriorityDateOriginFields(configurable, priorityDateOriginEarliest, nextHearingDate),
                PRIORITY_DATE_TYPE,
                configurable
            );

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWithOriginRefDateProvided(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();


        var configurationDmnEvaluationResponse = priorityDateOriginEarliestCalculator.calculateDate(
            readPriorityDateOriginFields(
                configurable,
                priorityDateOriginEarliest,
                nextHearingDate,
                priorityDate
            ),
            PRIORITY_DATE_TYPE,
            configurable
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(localDateTime + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWithOriginRefDateProvidedAndIntervalIsGreaterThan0(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginEarliestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   configurable,
                                                                   priorityDateOriginEarliest,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   priorityDateIntervalDays
                                                               ),
                                                               PRIORITY_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWithOriginEarliestDateProvidedAndGivenHolidays(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginEarliestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   configurable,
                                                                   priorityDateOriginEarliest,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateIntervalDays,
                                                                   priorityDateSkipNonWorkingDays
                                                               ),
                                                               PRIORITY_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(5)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWithOriginEarliestDateProvidedAndSkipNonWorkingDaysFalse(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginEarliestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   configurable,
                                                                   priorityDateOriginEarliest,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   priorityDateIntervalDays,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateSkipNonWorkingDays
                                                               ),
                                                               PRIORITY_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        String dateValue = priorityDateOriginEarliestCalculator.calculateDate(
            readPriorityDateOriginFields(
                configurable,
                priorityDateOriginEarliest,
                nextHearingDate,
                priorityDate,
                priorityDateMustBeWorkingDay,
                priorityDateNonWorkingDaysOfWeek,
                priorityDateSkipNonWorkingDays,
                priorityDateIntervalDays
            ),
            PRIORITY_DATE_TYPE,
            configurable
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedPriorityDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean configurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();

        var priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable)).build();


        var configurationDmnEvaluationResponse = priorityDateOriginEarliestCalculator.calculateDate(
            readPriorityDateOriginFields(
                configurable,
                priorityDateOriginEarliest,
                nextHearingDate,
                priorityDate,
                priorityDateIntervalDays,
                priorityDateMustBeWorkingDay,
                priorityDateNonWorkingDaysOfWeek,
                priorityDateSkipNonWorkingDays
            ),
            PRIORITY_DATE_TYPE,
            configurable
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readPriorityDateOriginFields(
        boolean configurable,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(configurable))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}
