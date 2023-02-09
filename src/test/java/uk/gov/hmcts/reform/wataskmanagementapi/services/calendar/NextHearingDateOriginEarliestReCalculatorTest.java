package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@ExtendWith(MockitoExtension.class)
class NextHearingDateOriginEarliestReCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static final boolean IS_RECONFIGURE_REQUEST = true;
    public static final DateTypeConfigurator.DateTypeObject NEXT_HEARING_DATE_TYPE = new DateTypeConfigurator.DateTypeObject(
        NEXT_HEARING_DATE,
        NEXT_HEARING_DATE.getType()
    );

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private NextHearingDateOriginEarliestReCalculator nextHearingDateOriginEarliestReCalculator;

    @BeforeEach
    public void before() {
        nextHearingDateOriginEarliestReCalculator
            = new NextHearingDateOriginEarliestReCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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

    @Test
    void should_not_supports_when_responses_contains_next_hearing_date() {

        var nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDateOrigin,
            nextHearingDateIntervalDays,
            nextHearingDateNonWorkingCalendar,
            nextHearingDateNonWorkingDaysOfWeek,
            nextHearingDateSkipNonWorkingDays,
            nextHearingDateMustBeWorkingDay,
            nextHearingDateTime
        );

        assertThat(nextHearingDateOriginEarliestReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_next_hearing_date_origin() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateOrigin);

        assertThat(nextHearingDateOriginEarliestReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }


    @Test
    void should_supports_when_responses_only_contains_next_hearing_date_origin_ref() {
        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateOriginEarliest);

        assertThat(nextHearingDateOriginEarliestReCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void shouldCalculateNotNullWhenOriginRefDateValueProvided() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var configurationDmnEvaluationResponse = nextHearingDateOriginEarliestReCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readNextHearingDateOriginFields(nextHearingDateOriginEarliest, priorityDate));

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithEarliestOriginRefDateProvided() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        var configurationDmnEvaluationResponse = nextHearingDateOriginEarliestReCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readNextHearingDateOriginFields(
                nextHearingDateOriginEarliest,
                dueDate,
                priorityDate
            )
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(localDateTime + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndIntervalIsGreaterThan0() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        String nextHearingDateValue = nextHearingDateOriginEarliestReCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readNextHearingDateOriginFields(
                    nextHearingDateOriginEarliest,
                    nextHearingDate,
                    calculatedDate,
                    dueDate,
                    nextHearingDateIntervalDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedNextHearingDate = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndGivenHolidays() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        String nextHearingDateValue = nextHearingDateOriginEarliestReCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readNextHearingDateOriginFields(
                    nextHearingDateOriginEarliest,
                    priorityDate,
                    calculatedDate,
                    dueDate,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateIntervalDays,
                    nextHearingDateSkipNonWorkingDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedNextHearingDate = GIVEN_DATE.plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndSkipNonWorkingDaysFalse() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        String nextHearingDateValue = nextHearingDateOriginEarliestReCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readNextHearingDateOriginFields(
                    nextHearingDateOriginEarliest,
                    dueDate,
                    priorityDate,
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedPriorityDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        String dateValue = nextHearingDateOriginEarliestReCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readNextHearingDateOriginFields(
                nextHearingDateOriginEarliest,
                priorityDate,
                calculatedDate,
                dueDate,
                nextHearingDateMustBeWorkingDay,
                nextHearingDateNonWorkingDaysOfWeek,
                nextHearingDateSkipNonWorkingDays,
                nextHearingDateIntervalDays
            )
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var configurationDmnEvaluationResponse = nextHearingDateOriginEarliestReCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readNextHearingDateOriginFields(
                nextHearingDateOriginEarliest,
                priorityDate,
                dueDate,
                nextHearingDateIntervalDays,
                nextHearingDateMustBeWorkingDay,
                nextHearingDateNonWorkingDaysOfWeek,
                nextHearingDateSkipNonWorkingDays
            )
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readNextHearingDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }

}
