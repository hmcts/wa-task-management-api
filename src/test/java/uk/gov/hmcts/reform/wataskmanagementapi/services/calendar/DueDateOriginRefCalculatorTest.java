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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculatorTest.DUE_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class DueDateOriginRefCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private DueDateOriginRefCalculator dueDateOriginRefCalculator;

    @BeforeEach
    public void before() {
        dueDateOriginRefCalculator = new DueDateOriginRefCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
    @CsvSource({"true","false"})
    void should_not_supports_when_responses_contains_due_date(boolean configurable) {

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
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            dueDateOrigin,
            dueDateIntervalDays,
            dueDateNonWorkingCalendar,
            dueDateNonWorkingDaysOfWeek,
            dueDateSkipNonWorkingDays,
            dueDateMustBeWorkingDay,
            dueDateTime
        );

        assertThat(dueDateOriginRefCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true","false"})
    void should_not_supports_when_responses_contains_due_date_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin);

        assertThat(dueDateOriginRefCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isFalse();
    }


    @ParameterizedTest
    @CsvSource({"true","false"})
    void should_supports_when_responses_only_contains_due_date_origin_ref(boolean configurable) {
        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOriginRef, dueDateTime);

        assertThat(dueDateOriginRefCalculator.supports(evaluationResponses, DUE_DATE_TYPE, configurable)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateNotNullWhenOriginDateValueProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var configurationDmnEvaluationResponse = dueDateOriginRefCalculator
            .calculateDate(readDueDateOriginFields(dueDateOriginRef, nextHearingDate), DUE_DATE_TYPE, configurable);

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWithOriginRefDateProvided(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
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


        var configurationDmnEvaluationResponse = dueDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                dueDateOriginRef,
                nextHearingDate,
                priorityDate
            ),
            DUE_DATE_TYPE,
            configurable
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(latestDateTime + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndIntervalIsGreaterThan0(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,dueDate"))
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

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   dueDateOriginRef,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nextHearingDate,
                                                                   dueDateIntervalDays
                                                               ),
                                                               DUE_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndGivenHolidays(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("priorityDate,nexHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nexHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nexHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   dueDateOriginRef,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nexHearingDate,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateIntervalDays,
                                                                   dueDateSkipNonWorkingDays
                                                               ),
                                                               DUE_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWithOriginRefDateProvidedAndSkipNonWorkingDaysFalse(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
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

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginRefCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   dueDateOriginRef,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateIntervalDays,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays
                                                               ),
                                                               DUE_DATE_TYPE,
                                                               configurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,dueDate"))
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
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        String dateValue = dueDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                dueDateOriginRef,
                priorityDate,
                calculatedDate,
                nextHearingDate,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays,
                dueDateIntervalDays
            ),
            DUE_DATE_TYPE,
            configurable
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({"true,T18:00","false,T18:00"})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean configurable, String time) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
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

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();
        var dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var configurationDmnEvaluationResponse = dueDateOriginRefCalculator.calculateDate(
            readDueDateOriginFields(
                dueDateOriginRef,
                priorityDate,
                nextHearingDate,
                dueDateIntervalDays,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays
            ),
            DUE_DATE_TYPE,
            configurable
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedDueDate + time);
    }

    private List<ConfigurationDmnEvaluationResponse> readDueDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }

}
