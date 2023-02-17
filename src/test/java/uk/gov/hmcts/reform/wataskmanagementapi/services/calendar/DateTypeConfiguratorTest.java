package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;

@ExtendWith(MockitoExtension.class)
public class DateTypeConfiguratorTest {
    ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("dueDate"))
        .value(CamundaValue.stringValue("2023-01-10T16:00"))
        .build();
    ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("priorityDate"))
        .value(CamundaValue.stringValue("2023-01-12T16:00"))
        .build();
    ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("nextHearingDate"))
        .value(CamundaValue.stringValue("2023-01-15T16:00"))
        .build();
    String calculatedDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";

    private DateTypeConfigurator dateTypeConfigurator;

    @Nested
    class DefaultWithoutAnyDateCalculator {


        @BeforeEach
        public void setUp() {
            dateTypeConfigurator = new DateTypeConfigurator(List.of());
        }

        @Test
        public void should_not_calculate_when_there_are_no_dmn_responses() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            dateTypeConfigurator.configureDates(evaluationResponses, false, false);
            assertThat(evaluationResponses).isEmpty();
        }

        @Test
        public void should_not_calculate_when_there_are_no_dmn_responses_and_initiation_due_date_found() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            dateTypeConfigurator.configureDates(evaluationResponses, true, false);
            assertThat(evaluationResponses).isEmpty();
        }

        @Test
        public void should_set_default_for_due_date_and_priority_date_when_only_due_date_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate);
            var output = dateTypeConfigurator.configureDates(input, false, false);
            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(calculatedDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(calculatedDate))
                                   .build()
                           )
                );
        }

        @Test
        public void should_set_default_for_due_date_and_priority_date_if_both_due_date_and_priority_dates_are_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate, priorityDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(
                input,
                false,
                false
            );

            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(calculatedDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(calculatedDate))
                                   .build()
                           )
                );
        }
    }

    @Nested
    class DefaultWithDateCalculators {

        @BeforeEach
        public void setUp() {
            dateTypeConfigurator = new DateTypeConfigurator(List.of(
                new DueDateCalculator(),
                new PriorityDateCalculator(),
                new NextHearingDateCalculator()
            ));
        }

        @Test
        public void should_not_calculate_when_there_are_no_dmn_responses() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            dateTypeConfigurator.configureDates(evaluationResponses, false, false);
            assertThat(evaluationResponses).isEmpty();
        }

        @Test
        public void should_not_calculate_when_there_are_no_dmn_responses_and_initiation_due_date_found() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            dateTypeConfigurator.configureDates(evaluationResponses, true, false);
            assertThat(evaluationResponses).isEmpty();
        }

        @Test
        public void should_set_default_for_due_date_and_priority_date_when_only_due_date_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(input, false, false);
            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .build()
                           )
                );
        }

        @Test
        public void should_set_calculates_for_due_date_and_priority_date_for_their_values() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate, priorityDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(
                input,
                false,
                false
            );

            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(priorityDate.getValue().getValue()))
                                   .build()
                           )
                );
        }
    }
}
