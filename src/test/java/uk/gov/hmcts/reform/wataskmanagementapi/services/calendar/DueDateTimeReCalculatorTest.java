package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;

@ExtendWith(MockitoExtension.class)
class DueDateTimeReCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;

    private DueDateTimeReCalculator dueDateTimeCalculator;


    @BeforeEach
    public void before() {
        dueDateTimeCalculator = new DueDateTimeReCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDateTime);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_unconfigurable_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, dueDateTime);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isTrue();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime, dueDateOrigin);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_unconfigurable_due_date_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime, dueDateOrigin);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isTrue();
    }

    @Test
    void should_supports_when_responses_only_contains_due_date_time() {
        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isTrue();
    }

    @Test
    void should_not_supports_when_responses_only_contains_unconfigurable_due_date_time() {
        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        Assertions.assertThat(dueDateTimeCalculator.supports(evaluationResponses, IS_RECONFIGURE_REQUEST)).isFalse();
    }

    @Test
    void should_calculate_due_date_when_time_is_given() {

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(dueDateTimeCalculator.calculateDueDate(evaluationResponses))
            .isEqualTo(expectedDueDate + "T16:00");
    }


    @Test
    void should_calculate_due_date_from_last_entry_when_multiple_time_is_given() {

        ConfigurationDmnEvaluationResponse dueDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .build();

        ConfigurationDmnEvaluationResponse dueDateTime2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDateTime, dueDateTime2);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Assertions.assertThat(dueDateTimeCalculator.calculateDueDate(evaluationResponses))
            .isEqualTo(expectedDueDate + "T20:00");
    }
}
