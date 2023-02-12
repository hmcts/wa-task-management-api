package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.IntermediateDateCalculatorTest.INTERMEDIATE_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class IntermediateDateTimeCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);

    private IntermediateDateTimeCalculator intermediateDateTimeCalculator;

    @BeforeEach
    public void before() {
        intermediateDateTimeCalculator = new IntermediateDateTimeCalculator();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_contains_due_date(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDuration,
            nextHearingDurationTime
        );

        assertThat(intermediateDateTimeCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_contains_unconfigurable_due_date(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDuration,
            nextHearingDurationTime
        );

        assertThat(intermediateDateTimeCalculator.supports(
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

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationTime,
            nextHearingDurationOrigin
        );

        assertThat(intermediateDateTimeCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_contains_unconfigurable_due_date_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationTime,
            nextHearingDurationOrigin
        );

        assertThat(intermediateDateTimeCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_supports_when_responses_only_contains_due_date_time(boolean configurable) {
        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDurationTime);

        assertThat(intermediateDateTimeCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void should_not_supports_when_responses_only_contains_unconfigurable_due_date_time(boolean configurable) {
        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDurationTime);

        assertThat(intermediateDateTimeCalculator.supports(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        )).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"true,T16:00", "false,T16:00"})
    void should_calculate_due_date_when_time_is_given(boolean configurable, String time) {

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDurationTime);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String responseValue = intermediateDateTimeCalculator.calculateDate(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(responseValue)).isEqualTo(expectedDueDate + time);
    }


    @ParameterizedTest
    @CsvSource({"true,T20:00", "false,T20:00"})
    void should_calculate_due_date_from_last_entry_when_multiple_time_is_given(boolean configurable, String time) {

        var nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        var nextHearingDurationTime2 = ConfigurationDmnEvaluationResponse.builder()
               .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            nextHearingDurationTime,
            nextHearingDurationTime2
        );

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String responseValue = intermediateDateTimeCalculator.calculateDate(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(responseValue)).isEqualTo(expectedDueDate + time);
    }
}
