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
class NextHearingDateOriginRefCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static final DateTypeConfigurator.DateTypeObject NEXT_HEARING_DATE_TYPE = new DateTypeConfigurator.DateTypeObject(
        NEXT_HEARING_DATE,
        NEXT_HEARING_DATE.getType()
    );

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private NextHearingDateOriginRefCalculator nextHearingDateOriginRefCalculator;

    @BeforeEach
    public void before() {
        nextHearingDateOriginRefCalculator
            = new NextHearingDateOriginRefCalculator(new WorkingDayIndicator(publicHolidaysCollection));

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
    void should_not_supports_when_responses_contains_priority_date() {

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("0"))
            .build();

        var nextHearingDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue(CALENDAR_URI))
            .build();
        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue(""))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("18:00"))
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

        assertThat(nextHearingDateOriginRefCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            false
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_priority_date_origin() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateOrigin);

        assertThat(nextHearingDateOriginRefCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            false
        )).isFalse();
    }


    @Test
    void should_supports_when_responses_only_contains_priority_date_origin_ref() {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDateOrigin, nextHearingDateTime);

        assertThat(nextHearingDateOriginRefCalculator.supports(
            evaluationResponses,
            NEXT_HEARING_DATE_TYPE,
            false
        )).isTrue();
    }

    @Test
    void shouldCalculateNotNullWhenOriginDateValueProvided() {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        var configurationDmnEvaluationResponse = nextHearingDateOriginRefCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readPriorityDateOriginFields(nextHearingDateOriginRef, nextHearingDate)
        );

        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvided() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        var configurationDmnEvaluationResponse = nextHearingDateOriginRefCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readPriorityDateOriginFields(
                nextHearingDateOriginRef,
                dueDate,
                priorityDate
            )
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndIntervalIsGreaterThan0() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,dueDate,nextHearingDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .build();


        String nextHearingDateValue = nextHearingDateOriginRefCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readPriorityDateOriginFields(
                    nextHearingDateOriginRef,
                    priorityDate,
                    calculatedDate,
                    dueDate,
                    nextHearingDateIntervalDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedPriorityDate = GIVEN_DATE.plusDays(3)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndGivenHolidays() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("priorityDate,dueDate,nextHearingDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .build();

        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .build();

        String nextHearingDateValue = nextHearingDateOriginRefCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readPriorityDateOriginFields(
                    nextHearingDateOriginRef,
                    priorityDate,
                    calculatedDate,
                    dueDate,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateIntervalDays,
                    nextHearingDateSkipNonWorkingDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedPriorityDate = GIVEN_DATE.plusDays(7)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWithOriginRefDateProvidedAndSkipNonWorkingDaysFalse() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();

        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();

        String nextHearingDateValue = nextHearingDateOriginRefCalculator
            .calculateDate(
                NEXT_HEARING_DATE_TYPE,
                readPriorityDateOriginFields(
                    nextHearingDateOriginRef,
                    dueDate,
                    priorityDate,
                    nextHearingDateIntervalDays,
                    nextHearingDateNonWorkingDaysOfWeek,
                    nextHearingDateSkipNonWorkingDays
                )
            ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateValue);

        String expectedPriorityDate = GIVEN_DATE.plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDate"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,nextHearingDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .build();
        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .build();

        String dateValue = nextHearingDateOriginRefCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readPriorityDateOriginFields(
                nextHearingDateOriginRef,
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

        String expectedPriorityDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @Test
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious() {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginRef"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .build();

        var nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .build();
        var nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .build();
        var nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .build();

        var configurationDmnEvaluationResponse = nextHearingDateOriginRefCalculator.calculateDate(
            NEXT_HEARING_DATE_TYPE,
            readPriorityDateOriginFields(
                nextHearingDateOriginRef,
                dueDate,
                priorityDate,
                nextHearingDateIntervalDays,
                nextHearingDateMustBeWorkingDay,
                nextHearingDateNonWorkingDaysOfWeek,
                nextHearingDateSkipNonWorkingDays
            )
        );
        LocalDateTime resultDate = LocalDateTime.parse(configurationDmnEvaluationResponse.getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readPriorityDateOriginFields(
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}
