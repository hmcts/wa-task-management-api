package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@ExtendWith(MockitoExtension.class)
class PriorityDateTimeReCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;

    private PriorityDateTimeCalculator priorityDateTimeReCalculator;


    @BeforeEach
    public void before() {
        priorityDateTimeReCalculator = new PriorityDateTimeCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_due_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDateTime);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_unconfigurable_priority_date() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDateTime);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_not_supports_when_responses_contains_priority_date_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime, priorityDateOrigin);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_contains_unconfigurable_priority_date_origin() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOrigin = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T22:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime, priorityDateOrigin);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_supports_when_responses_only_contains_priority_date_time() {
        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_not_supports_when_responses_only_contains_unconfigurable_priority_date_time() {
        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime);

        assertThat(priorityDateTimeReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_calculate_priority_date_when_time_is_given() {

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String responseValue = priorityDateTimeReCalculator.calculateDate(
            evaluationResponses,
            PRIORITY_DATE,
            true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(responseValue)).isEqualTo(expectedDueDate + "T16:00");
    }


    @Test
    void should_calculate_priority_date_from_last_entry_when_multiple_time_is_given() {

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime, priorityDateTime2);

        String expectedDueDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String responseValue = priorityDateTimeReCalculator.calculateDate(
            evaluationResponses,
            PRIORITY_DATE,
            true
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(responseValue)).isEqualTo(expectedDueDate + "T20:00");
    }
}
