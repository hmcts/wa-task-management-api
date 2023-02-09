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
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PriorityDateCalculatorTest.PRIORITY_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
class PriorityDateReCalculatorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 00, 00);
    public static final boolean IS_RECONFIGURE_REQUEST = true;

    private PriorityDateReCalculator priorityDateReCalculator;

    @BeforeEach
    public void before() {
        priorityDateReCalculator = new PriorityDateReCalculator();
    }

    @Test
    void should_not_supports_when_responses_contains_priority_date_origin_and_time() {
        String expectedDueDate = GIVEN_DATE.plusDays(0)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOrigin"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDateTime);

        assertThat(priorityDateReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_contains_only_priority_date_time() {

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDateTime);

        assertThat(priorityDateReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_not_supports_when_responses_only_contains_priority_date_with_can_configure_false_but_not_origin() {
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

        assertThat(priorityDateReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isFalse();
    }

    @Test
    void should_supports_when_responses_only_contains_priority_date_but_not_origin() {
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

        assertThat(priorityDateReCalculator.supports(
            evaluationResponses,
            PRIORITY_DATE_TYPE,
            IS_RECONFIGURE_REQUEST
        )).isTrue();
    }

    @Test
    void should_calculate_priority_date_when_priority_date_is_given() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate);

        String dateValue = priorityDateReCalculator.calculateDate(
            PRIORITY_DATE_TYPE, evaluationResponses
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + "T16:00");
    }

    @Test
    void should_consider_only_priority_date_when_given_configurable_priority_date_and_un_configurable_time() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(false))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDateTime);

        String dateValue = priorityDateReCalculator.calculateDate(
            PRIORITY_DATE_TYPE, evaluationResponses
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + "T16:00");
    }

    @Test
    void should_calculate_priority_date_when_priority_date_and_time_are_given() {

        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateTime = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateTime"))
            .value(CamundaValue.stringValue("20:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDateTime);

        String dateValue = priorityDateReCalculator.calculateDate(
            PRIORITY_DATE_TYPE, evaluationResponses
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate + "T20:00");
    }


    @Test
    void should_calculate_priority_date_from_last_entry_when_multiple_time_is_given() {
        String expectedDueDate = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String expectedDueDate2 = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate + "T16:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDate2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDate"))
            .value(CamundaValue.stringValue(expectedDueDate2 + "T19:00"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(priorityDate, priorityDate2);

        String dateValue = priorityDateReCalculator.calculateDate(
            PRIORITY_DATE_TYPE, evaluationResponses
        ).getValue().getValue();
        assertThat(LocalDateTime.parse(dateValue)).isEqualTo(expectedDueDate2 + "T19:00");
    }
}
