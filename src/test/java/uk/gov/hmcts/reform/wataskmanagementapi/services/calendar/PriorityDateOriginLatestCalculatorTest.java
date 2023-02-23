package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PriorityDateCalculatorTest.PRIORITY_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class PriorityDateOriginLatestCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private PriorityDateOriginLatestCalculator priorityDateOriginLatestCalculator;

    @BeforeEach
    public void before() {
        priorityDateOriginLatestCalculator = new PriorityDateOriginLatestCalculator(
            new WorkingDayIndicator(publicHolidaysCollection));

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
    @ValueSource(booleans = {true, false})
    void should_not_supports_when_responses_contains_priority_date(boolean isReconfigureRequest) {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate);

        Assertions.assertThat(priorityDateOriginLatestCalculator
                                  .supports(evaluationResponses, PRIORITY_DATE_TYPE, isReconfigureRequest))
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_not_supports_when_responses_contains_priority_date_origin(boolean isReconfigureRequest) {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateOrigin);

        Assertions.assertThat(priorityDateOriginLatestCalculator
                                  .supports(evaluationResponses, PRIORITY_DATE_TYPE, isReconfigureRequest))
            .isFalse();
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_supports_when_responses_only_contains_priority_date_origin_latest(boolean isReconfigureRequest) {
        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue(expectedPriorityDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            priorityDateOrigin,
            priorityDateTime
        );

        Assertions.assertThat(priorityDateOriginLatestCalculator
                                  .supports(evaluationResponses, PRIORITY_DATE_TYPE, isReconfigureRequest))
            .isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenOriginDateValueProvided(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateDateLatestOrigin,
                                                                   dueDate
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvided(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateLatestOrigin,
                                                                   dueDate,
                                                                   nextHearingDate
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        Assertions.assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndIntervalIsGreaterThan0(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateLatestOrigin,
                                                                   dueDate,
                                                                   nextHearingDate,
                                                                   priorityDateIntervalDays
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(5)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndGivenHolidays(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateLatestOrigin,
                                                                   dueDate,
                                                                   nextHearingDate,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateIntervalDays,
                                                                   priorityDateSkipNonWorkingDays
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(7)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndSkipNonWorkingDaysFalse(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateLatestOrigin,
                                                                   dueDate,
                                                                   nextHearingDate,
                                                                   priorityDateIntervalDays,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateSkipNonWorkingDays
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        String dateValue = priorityDateOriginLatestCalculator.calculateDate(
            readPriorityDateOriginFields(
                isReconfigureRequest,
                priorityDateLatestOrigin,
                dueDate,
                nextHearingDate,
                priorityDateMustBeWorkingDay,
                priorityDateNonWorkingDaysOfWeek,
                priorityDateSkipNonWorkingDays,
                priorityDateIntervalDays
            ),
            PRIORITY_DATE_TYPE, isReconfigureRequest
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedPriorityDate = GIVEN_DATE.plusDays(4)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse priorityDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(priorityDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readPriorityDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   priorityDateLatestOrigin,
                                                                   dueDate,
                                                                   nextHearingDate,
                                                                   priorityDateIntervalDays,
                                                                   priorityDateMustBeWorkingDay,
                                                                   priorityDateNonWorkingDaysOfWeek,
                                                                   priorityDateSkipNonWorkingDays
                                                               ),
                                                               PRIORITY_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedPriorityDate = GIVEN_DATE.plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedPriorityDate + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readPriorityDateOriginFields(
        boolean isReconfigureRequest,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}

