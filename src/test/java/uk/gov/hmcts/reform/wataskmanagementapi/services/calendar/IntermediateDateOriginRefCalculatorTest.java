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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.IntermediateDateCalculatorTest.INTERMEDIATE_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class IntermediateDateOriginRefCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final String LOCAL_DATE_TIME = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public List<ConfigurationDmnEvaluationResponse> calculatedConfigurations;
    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;
    private IntermediateDateOriginRefCalculator intermediateDateOriginRefCalculator;

    @BeforeEach
    void before() {
        calculatedConfigurations = new ArrayList<>();
        intermediateDateOriginRefCalculator = new IntermediateDateOriginRefCalculator(new WorkingDayIndicator(
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
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_due_date(boolean configurable) {

        var nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(LOCAL_DATE_TIME + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationOrigin,
            nextHearingDurationIntervalDays,
            nextHearingDurationNonWorkingCalendar,
            nextHearingDurationNonWorkingDaysOfWeek,
            nextHearingDurationSkipNonWorkingDays,
            nextHearingDurationMustBeWorkingDay,
            nextHearingDurationTime
        );

        assertThat(intermediateDateOriginRefCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_due_date_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDurationOrigin);

        assertThat(intermediateDateOriginRefCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isFalse();
    }


    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_only_contains_due_date_origin_ref(boolean configurable) {
        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDurationOriginRef, nextHearingDurationTime);

        assertThat(intermediateDateOriginRefCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateNotNullWhenOriginDateValueProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        var configurationDmnEvaluationResponse = intermediateDateOriginRefCalculator
            .calculateDate(
                readDueDateOriginFields(nextHearingDurationOriginRef, nextHearingDate),
                INTERMEDIATE_DATE_TYPE,
                configurable,
                new HashMap<>(),
                calculatedConfigurations
            );

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWithOriginRefDateProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);
        var configurationDmnEvaluationResponse = intermediateDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                nextHearingDurationOriginRef,
                nextHearingDate,
                priorityDate
            ),
            INTERMEDIATE_DATE_TYPE,
            configurable,
            new HashMap<>(),
            calculatedConfigurations
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(latestDateTime + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndIntervalIsGreaterThan0(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);

        LocalDateTime resultDate = LocalDateTime.parse(intermediateDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   nextHearingDurationOriginRef,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nextHearingDate,
                                                                   nextHearingDurationIntervalDays
                                                               ),
                                                               INTERMEDIATE_DATE_TYPE,
                                                               configurable,
                                                               new HashMap<>(),
                                                               calculatedConfigurations
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndGivenHolidays(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nexHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);
        LocalDateTime resultDate = LocalDateTime.parse(intermediateDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   nextHearingDurationOriginRef,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nextHearingDate,
                                                                   nextHearingDurationNonWorkingDaysOfWeek,
                                                                   nextHearingDurationIntervalDays,
                                                                   nextHearingDurationSkipNonWorkingDays
                                                               ),
                                                               INTERMEDIATE_DATE_TYPE,
                                                               configurable,
                                                               new HashMap<>(),
                                                               calculatedConfigurations
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndSkipNonWorkingDaysFalse(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);
        LocalDateTime resultDate = LocalDateTime.parse(intermediateDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   nextHearingDurationOriginRef,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   nextHearingDurationIntervalDays,
                                                                   nextHearingDurationNonWorkingDaysOfWeek,
                                                                   nextHearingDurationSkipNonWorkingDays
                                                               ),
                                                               INTERMEDIATE_DATE_TYPE,
                                                               configurable,
                                                               new HashMap<>(),
                                                               calculatedConfigurations
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,nextHearingDuration"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);
        String dateValue = intermediateDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                nextHearingDurationOriginRef,
                priorityDate,
                calculatedDate,
                nextHearingDate,
                nextHearingDurationMustBeWorkingDay,
                nextHearingDurationNonWorkingDaysOfWeek,
                nextHearingDurationSkipNonWorkingDays,
                nextHearingDurationIntervalDays
            ),
            INTERMEDIATE_DATE_TYPE,
            configurable,
            new HashMap<>(),
            calculatedConfigurations
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00", "false,T18:00"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var nextHearingDurationMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        calculatedConfigurations.add(nextHearingDate);
        calculatedConfigurations.add(priorityDate);
        var configurationDmnEvaluationResponse = intermediateDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                nextHearingDurationOriginRef,
                priorityDate,
                nextHearingDate,
                nextHearingDurationIntervalDays,
                nextHearingDurationMustBeWorkingDay,
                nextHearingDurationNonWorkingDaysOfWeek,
                nextHearingDurationSkipNonWorkingDays
            ),
            INTERMEDIATE_DATE_TYPE,
            configurable,
            new HashMap<>(),
            calculatedConfigurations
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    private List<ConfigurationDmnEvaluationResponse> readDueDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDurationTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}
