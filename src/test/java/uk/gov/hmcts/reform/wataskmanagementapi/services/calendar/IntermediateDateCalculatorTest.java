package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@ExtendWith(MockitoExtension.class)
class IntermediateDateCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final DateTypeObject INTERMEDIATE_DATE_TYPE = new DateTypeObject(
        INTERMEDIATE_DATE,
        "nextHearingDuration"
    );
    private IntermediateDateCalculator intermediateDateCalculator;


    @BeforeEach
    public void before() {
        intermediateDateCalculator = new IntermediateDateCalculator();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_due_date_origin_and_time(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDuration, nextHearingDurationTime);

        assertThat(intermediateDateCalculator.supports(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable))
            .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_not_supports_when_responses_contains_only_due_date_time(boolean configurable) {

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDurationTime);

        assertThat(intermediateDateCalculator.supports(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable))
            .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "true", "false"
    })
    void should_supports_when_responses_only_contains_due_date_but_not_origin(boolean configurable) {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDuration, nextHearingDurationTime);

        assertThat(intermediateDateCalculator.supports(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable))
            .isTrue();
    }


    @ParameterizedTest
    @CsvSource({
        "true, T16:00", "false, T16:00"
    })
    void should_calculate_due_date_when_due_date_is_given(boolean configurable, String time) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(nextHearingDuration);

        String dateValue = intermediateDateCalculator.calculateDate(
            evaluationResponses,
            INTERMEDIATE_DATE_TYPE,
            configurable
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_consider_only_due_date_when_given_configurable_due_date_and_un_configurable_time(
        boolean configurable, String time) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDuration, nextHearingDurationTime);

        String dateValue = intermediateDateCalculator
            .calculateDate(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable)
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + time);
    }

    @ParameterizedTest
    @CsvSource({
        "true, T20:00", "false, T20:00"
    })
    void should_calculate_due_date_when_due_date_and_time_are_given(boolean configurable, String time) {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDurationTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDurationTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDuration, nextHearingDurationTime);

        String dateValue = intermediateDateCalculator
            .calculateDate(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable)
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + time);
    }


    @ParameterizedTest
    @CsvSource({
        "true, T19:00", "false, T19:00"
    })
    void should_calculate_due_date_from_last_entry_when_multiple_time_is_given(boolean configurable, String time) {
        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedDueDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDuration = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        ConfigurationDmnEvaluationResponse nextHearingDuration2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDuration"))
            .value(CamundaValue.stringValue(expectedDueDate2 + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(configurable))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses
            = List.of(nextHearingDuration, nextHearingDuration2);

        String dateValue = intermediateDateCalculator
            .calculateDate(evaluationResponses, INTERMEDIATE_DATE_TYPE, configurable)
            .getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate2 + time);
    }
}
