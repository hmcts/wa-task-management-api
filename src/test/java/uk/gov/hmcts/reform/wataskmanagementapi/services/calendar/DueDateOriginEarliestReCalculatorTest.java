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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@ExtendWith(MockitoExtension.class)
class DueDateOriginEarliestReCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static final boolean IS_RECONFIGURE_REQUEST = true;

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private DueDateOriginEarliestReCalculator dueDateOriginEarliestReCalculator;

    @BeforeEach
    public void before() {
        dueDateOriginEarliestReCalculator
            = new DueDateOriginEarliestReCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
    void should_not_supports_when_responses_contains_due_date() {

        var dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
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

        assertThat(dueDateOriginEarliestReCalculator
                       .supports(evaluationResponses, DUE_DATE, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date_origin() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin);

        assertThat(dueDateOriginEarliestReCalculator.supports(
            evaluationResponses,
            DUE_DATE,
            IS_RECONFIGURE_REQUEST
        ))
            .isFalse();
    }


    @Test
    void should_supports_when_responses_only_contains_due_date_origin_ref() {
        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOriginEarliest);

        assertThat(dueDateOriginEarliestReCalculator.supports(
            evaluationResponses,
            DUE_DATE,
            IS_RECONFIGURE_REQUEST
        ))
            .isTrue();
    }

    @Test
    void shouldCalculateNotNullWhenOriginRefDateValueProvided() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var configurationDmnEvaluationResponse = dueDateOriginEarliestReCalculator
            .calculateDate(DUE_DATE, readDueDateOriginFields(dueDateOriginEarliest, priorityDate)
            );

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithEarliestOriginRefDateProvided() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T18:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        var configurationDmnEvaluationResponse = dueDateOriginEarliestReCalculator.calculateDate(
            DUE_DATE, readDueDateOriginFields(
                dueDateOriginEarliest,
                nextHearingDate,
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

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginEarliestReCalculator
                                                           .calculateDate(
                                                               DUE_DATE, readDueDateOriginFields(
                                                                   dueDateOriginEarliest,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nextHearingDate,
                                                                   dueDateIntervalDays
                                                               )
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndGivenHolidays() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,nexHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nexHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nexHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginEarliestReCalculator
                                                           .calculateDate(
                                                               DUE_DATE, readDueDateOriginFields(
                                                                   dueDateOriginEarliest,
                                                                   priorityDate,
                                                                   calculatedDate,
                                                                   nexHearingDate,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateIntervalDays,
                                                                   dueDateSkipNonWorkingDays
                                                               )
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(7)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndSkipNonWorkingDaysFalse() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginEarliestReCalculator
                                                           .calculateDate(
                                                               DUE_DATE, readDueDateOriginFields(
                                                                   dueDateOriginEarliest,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateIntervalDays,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays
                                                               )
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
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

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        String dateValue = dueDateOriginEarliestReCalculator.calculateDate(
            DUE_DATE, readDueDateOriginFields(
                dueDateOriginEarliest,
                priorityDate,
                calculatedDate,
                nextHearingDate,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays,
                dueDateIntervalDays
            )
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedPriorityDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginEarliest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();
        var dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        var configurationDmnEvaluationResponse = dueDateOriginEarliestReCalculator.calculateDate(
            DUE_DATE, readDueDateOriginFields(
                dueDateOriginEarliest,
                priorityDate,
                nextHearingDate,
                dueDateIntervalDays,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays
            )
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
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
