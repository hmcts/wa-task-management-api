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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculatorTest.DUE_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class DueDateOriginLatestCalculatorTest {

    public static final String CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    private DueDateOriginLatestCalculator dueDateOriginLatestCalculator;

    @BeforeEach
    public void before() {
        dueDateOriginLatestCalculator = new DueDateOriginLatestCalculator(
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
    void should_not_supports_when_responses_contains_due_date(boolean isConfigurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate);

        Assertions.assertThat(dueDateOriginLatestCalculator
                                  .supports(evaluationResponses, DUE_DATE_TYPE, isConfigurable))
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_not_supports_when_responses_contains_due_date_origin(boolean isConfigurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin);

        Assertions.assertThat(dueDateOriginLatestCalculator
                                  .supports(evaluationResponses, DUE_DATE_TYPE, isConfigurable))
            .isFalse();
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_supports_when_responses_only_contains_due_date_origin_latest(boolean isConfigurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateOrigin, dueDateTime);

        Assertions.assertThat(dueDateOriginLatestCalculator
                                  .supports(evaluationResponses, DUE_DATE_TYPE, isConfigurable))
            .isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateNotNullWhenOriginDateValueProvided(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvided(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate,
                                                                   priorityDate
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        Assertions.assertThat(resultDate).isEqualTo(latestDateTime + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndIntervalIsGreaterThan0(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("priorityDate,nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("3"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();


        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateIntervalDays
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(5)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndGivenHolidays(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateIntervalDays,
                                                                   dueDateSkipNonWorkingDays
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(7)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWithLatestOriginDateProvidedAndSkipNonWorkingDaysFalse(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
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

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateIntervalDays,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessNext(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();
        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
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
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        String dateValue = dueDateOriginLatestCalculator.calculateDate(
            readDueDateOriginFields(
                isConfigurable,
                dueDateLatestOrigin,
                nextHearingDate,
                priorityDate,
                dueDateMustBeWorkingDay,
                dueDateNonWorkingDaysOfWeek,
                dueDateSkipNonWorkingDays,
                dueDateIntervalDays
            ),
            DUE_DATE_TYPE, isConfigurable
        ).getValue().getValue();
        LocalDateTime resultDate = LocalDateTime.parse(dateValue);

        String expectedDueDate = GIVEN_DATE.plusDays(4)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCalculateWhenSkipNonWorkingDaysAndMustBeBusinessPrevious(boolean isConfigurable) {
        String localDateTime = GIVEN_DATE.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String latestDateTime = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateLatestOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginLatest"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(localDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(latestDateTime + "T20:00"))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("2"))
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
            .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS))
            .canReconfigure(CamundaValue.booleanValue(isConfigurable))
            .build();

        LocalDateTime resultDate = LocalDateTime.parse(dueDateOriginLatestCalculator
                                                           .calculateDate(
                                                               readDueDateOriginFields(
                                                                   isConfigurable,
                                                                   dueDateLatestOrigin,
                                                                   nextHearingDate,
                                                                   priorityDate,
                                                                   dueDateIntervalDays,
                                                                   dueDateMustBeWorkingDay,
                                                                   dueDateNonWorkingDaysOfWeek,
                                                                   dueDateSkipNonWorkingDays
                                                               ),
                                                               DUE_DATE_TYPE, isConfigurable
                                                           ).getValue().getValue());

        String expectedDueDate = GIVEN_DATE.plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertThat(resultDate).isEqualTo(expectedDueDate + "T18:00");
    }

    private List<ConfigurationDmnEvaluationResponse> readDueDateOriginFields(
        boolean isConfigurable,
        ConfigurationDmnEvaluationResponse... fields) {
        List<ConfigurationDmnEvaluationResponse> allFields = new ArrayList<>(List.of(
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateIntervalDays"))
                .value(CamundaValue.stringValue("0"))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
                .value(CamundaValue.stringValue(CALENDAR_URI))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
                .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
                .value(CamundaValue.stringValue("true"))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
                .value(CamundaValue.stringValue(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build(),
            ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateTime"))
                .value(CamundaValue.stringValue("18:00"))
                .canReconfigure(CamundaValue.booleanValue(isConfigurable))
                .build()
        ));
        allFields.addAll(List.of(fields));

        return allFields;
    }
}
