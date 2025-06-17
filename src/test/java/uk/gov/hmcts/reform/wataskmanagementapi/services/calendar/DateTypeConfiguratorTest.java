package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_DATE;

@ExtendWith(MockitoExtension.class)
class DateTypeConfiguratorTest {
    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    private final Map<String, Object> taskAttributes = new HashMap<>();
    ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("dueDate"))
        .value(CamundaValue.stringValue("2023-01-10T16:00"))
        .build();
    ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("priorityDate"))
        .value(CamundaValue.stringValue("2023-01-12T16:00"))
        .build();
    String defaultDate = DEFAULT_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "T16:00";
    private DateTypeConfigurator dateTypeConfigurator;

    @Nested
    class DefaultWithoutAnyDateCalculator {


        @BeforeEach
        void setUp() {
            dateTypeConfigurator = new DateTypeConfigurator(List.of());
        }

        @Test
        void should_return_nextHearingDate_empty() {
            ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDate"))
                .value(CamundaValue.stringValue(""))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build();
            //create a list of dmn responses including nextHearingDate which is set to null
            // e.g. when camunda value is empty for nextHearingDate it should set the value to empty
            List<ConfigurationDmnEvaluationResponse> evaluationResponses =
                List.of(nextHearingDate, dueDate, priorityDate);

            //call the configureDates method using the list of dmn responses
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                false,
                true,
                taskAttributes
            );

            //assert that the dmnEvaluationResponses contains the nextHearingDate with an empty value
            assertThat(dmnEvaluationResponses)
                .hasSize(1)
                .contains(
                    nextHearingDate
                );
        }

        @Test
        void should_return_default_calculated_dates_when_there_are_no_dmn_responses() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            );
            assertThat(dmnEvaluationResponses)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build()
                           )
                );
        }

        @Test
        void should_not_calculate_when_there_are_no_dmn_responses_and_initiation_due_date_found() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                true,
                false,
                taskAttributes
            );
            assertThat(dmnEvaluationResponses).isEmpty();
        }

        @Test
        void should_set_default_for_due_date_and_priority_date_when_only_due_date_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate);
            var output = dateTypeConfigurator.configureDates(input, false, false, taskAttributes);
            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build()
                           )
                );
        }

        @Test
        void should_set_default_for_due_date_and_priority_date_if_both_due_date_and_priority_dates_are_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate, priorityDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(
                input,
                false,
                false,
                taskAttributes
            );

            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build()
                           )
                );
        }
    }

    @Nested
    class DefaultWithDateCalculators {

        @BeforeEach
        void setUp() {
            dateTypeConfigurator = new DateTypeConfigurator(List.of(
                new DueDateCalculator(),
                new PriorityDateCalculator(),
                new NextHearingDateCalculator()
            ));
        }

        @Test
        void should_return_default_calculated_dates_when_there_are_no_dmn_responses() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                false,
                false,
                taskAttributes
            );
            assertThat(dmnEvaluationResponses)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(defaultDate))
                                   .build()
                           )
                );
        }

        @Test
        void should_not_calculate_when_there_are_no_dmn_responses_and_initiation_due_date_found() {
            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of();
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                true,
                false,
                taskAttributes
            );
            assertThat(dmnEvaluationResponses).isEmpty();
        }

        @Test
        void should_set_default_for_due_date_and_priority_date_when_only_due_date_given() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(input, false, false,
                                                                                                  taskAttributes
            );
            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .canReconfigure(CamundaValue.booleanValue(false))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .build()
                           )
                );
        }

        @Test
        void should_set_calculates_for_due_date_and_priority_date_for_their_values() {
            List<ConfigurationDmnEvaluationResponse> input = List.of(dueDate, priorityDate);
            List<ConfigurationDmnEvaluationResponse> output = dateTypeConfigurator.configureDates(
                input,
                false,
                false,
                taskAttributes
            );

            assertThat(output)
                .hasSize(2)
                .isEqualTo(List.of(
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("dueDate"))
                                   .value(CamundaValue.stringValue(dueDate.getValue().getValue()))
                                   .canReconfigure(CamundaValue.booleanValue(false))
                                   .build(),
                               ConfigurationDmnEvaluationResponse.builder()
                                   .name(CamundaValue.stringValue("priorityDate"))
                                   .value(CamundaValue.stringValue(priorityDate.getValue().getValue()))
                                   .canReconfigure(CamundaValue.booleanValue(false))
                                   .build()
                           )
                );
        }
    }

    @Nested
    class ReconfigurationDateCalculationForOriginRef {

        @Mock
        WorkingDayIndicator workingDayIndicator;

        @BeforeEach
        void setUp() {
            dateTypeConfigurator = new DateTypeConfigurator(List.of(
                new DueDateCalculator(),
                new NextHearingDateCalculator(),
                new PriorityDateCalculator(),
                new DueDateIntervalCalculator(workingDayIndicator),
                new DueDateOriginEarliestCalculator(workingDayIndicator)
            ));
        }

        @Test
        void should_calculate_using_task_resource_attributes() {
            String nextHearingDateValue = GIVEN_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            String dueDateValue = GIVEN_DATE.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateTime taskResourceDueDate = GIVEN_DATE.minusDays(4);
            taskAttributes.put("priorityDate", taskResourceDueDate.atZone(ZoneId.systemDefault()).toOffsetDateTime());

            ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("calculatedDates"))
                .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
                .build();

            ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("nextHearingDate"))
                .value(CamundaValue.stringValue(nextHearingDateValue))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build();

            ConfigurationDmnEvaluationResponse priorityDate9Pm = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("priorityDate"))
                .value(CamundaValue.stringValue(dueDateValue + "T21:00"))
                .canReconfigure(CamundaValue.booleanValue(false))
                .build();

            ConfigurationDmnEvaluationResponse dueDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue("dueDateOriginEarliest"))
                .value(CamundaValue.stringValue("nextHearingDate,priorityDate"))
                .canReconfigure(CamundaValue.booleanValue(true))
                .build();

            List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
                calculatedDates,
                nextHearingDate,
                priorityDate9Pm,
                dueDateOriginEarliest
            );
            List<ConfigurationDmnEvaluationResponse> dmnEvaluationResponses = dateTypeConfigurator.configureDates(
                evaluationResponses,
                false,
                true,
                taskAttributes
            );
            assertThat(dmnEvaluationResponses).hasSize(2)
                .isEqualTo(List.of(
                    ConfigurationDmnEvaluationResponse.builder()
                        .name(CamundaValue.stringValue("nextHearingDate"))
                        .value(CamundaValue.stringValue(nextHearingDateValue))
                        .canReconfigure(CamundaValue.booleanValue(true))
                        .build(),
                    ConfigurationDmnEvaluationResponse.builder()
                        .name(CamundaValue.stringValue("dueDate"))
                        .value(CamundaValue.stringValue(taskResourceDueDate
                                                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                                            + "T18:00"))
                        .canReconfigure(CamundaValue.booleanValue(true))
                        .build()
                ));
        }
    }
}
