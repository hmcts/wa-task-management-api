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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.NextHearingDateCalculatorTest.NEXT_HEARING_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class NextHearingDateOriginLatestCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private NextHearingDateOriginLatestCalculator nextHearingDateOriginLatestCalculator;

    @BeforeEach
    public void before() {
        nextHearingDateOriginLatestCalculator = new NextHearingDateOriginLatestCalculator(
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
    void should_not_supports_when_responses_contains_next_hearing_date(boolean isReconfigureRequest) {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDate);

        Assertions.assertThat(nextHearingDateOriginLatestCalculator
                                  .supports(evaluationResponses, NEXT_HEARING_DATE_TYPE, isReconfigureRequest))
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_not_supports_when_responses_contains_next_hearing_date_origin(boolean isReconfigureRequest) {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOrigin"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDateOrigin);

        Assertions.assertThat(nextHearingDateOriginLatestCalculator
                                  .supports(evaluationResponses, NEXT_HEARING_DATE_TYPE, isReconfigureRequest))
            .isFalse();
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_supports_when_responses_only_contains_next_hearing_date_origin_latest(boolean isReconfigureRequest) {
        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue(expectedNextHearingDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDateOrigin,
            nextHearingDateTime
        );

        Assertions.assertThat(nextHearingDateOriginLatestCalculator
                                  .supports(evaluationResponses, NEXT_HEARING_DATE_TYPE, isReconfigureRequest))
            .isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenOriginDateValueProvided(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateDateLatestOrigin = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateDateLatestOrigin,
                                                                   dueDate
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvided(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateLatestOrigin,
                                                                   dueDate,
                                                                   priorityDate
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        Assertions.assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndIntervalIsGreaterThan0(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("priorityDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
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
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateLatestOrigin,
                                                                   dueDate,
                                                                   priorityDate,
                                                                   nextHearingDateIntervalDays
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(3)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndGivenHolidays(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateLatestOrigin,
                                                                   dueDate,
                                                                   priorityDate,
                                                                   nextHearingDateNonWorkingDaysOfWeek,
                                                                   nextHearingDateIntervalDays,
                                                                   nextHearingDateSkipNonWorkingDays
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(7)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndSkipNonWorkingDaysFalse(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
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
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateLatestOrigin,
                                                                   dueDate,
                                                                   priorityDate,
                                                                   nextHearingDateIntervalDays,
                                                                   nextHearingDateNonWorkingDaysOfWeek,
                                                                   nextHearingDateSkipNonWorkingDays
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
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
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        String dateValue = nextHearingDateOriginLatestCalculator.calculateDate(
            readNextHearingDateOriginFields(
                isReconfigureRequest,
                nextHearingDateLatestOrigin,
                dueDate,
                priorityDate,
                nextHearingDateMustBeWorkingDay,
                nextHearingDateNonWorkingDaysOfWeek,
                nextHearingDateSkipNonWorkingDays,
                nextHearingDateIntervalDays
            ),
            NEXT_HEARING_DATE_TYPE, isReconfigureRequest
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedNextHearingDate = GIVEN_DATE.plusDays(4)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean isReconfigureRequest) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDateOriginLatest"))
            .value(CamundaValue.stringValue("dueDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
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
            .value(CamundaValue.stringValue("2"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("false"))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
        ConfigurationDmnEvaluationResponse nextHearingDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(nextHearingDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readNextHearingDateOriginFields(
                                                                   isReconfigureRequest,
                                                                   nextHearingDateLatestOrigin,
                                                                   dueDate,
                                                                   priorityDate,
                                                                   nextHearingDateIntervalDays,
                                                                   nextHearingDateMustBeWorkingDay,
                                                                   nextHearingDateNonWorkingDaysOfWeek,
                                                                   nextHearingDateSkipNonWorkingDays
                                                               ),
                                                               NEXT_HEARING_DATE_TYPE, isReconfigureRequest
                                                           ).getValue().getValue());

        String expectedNextHearingDate = GIVEN_DATE.plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedNextHearingDate + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readNextHearingDateOriginFields(
        boolean isReconfigureRequest,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}

