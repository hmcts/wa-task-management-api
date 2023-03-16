package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.DateCalculationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.MANDATORY_DATES_NOT_IN_REQUIRED_ORDER_IN_CALCULATED_DATES;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.MANDATORY_DATES_NOT_PROVIDED_IN_CALCULATED_DATES;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculatorTest.DUE_DATE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.IntermediateDateCalculatorTest.INTERMEDIATE_DATE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.NextHearingDateCalculatorTest.NEXT_HEARING_DATE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.PriorityDateCalculatorTest.PRIORITY_DATE_TYPE;

@ExtendWith(MockitoExtension.class)
public class DateTypeConfiguratorOrderTest {
    public static final ArrayList<ConfigurationDmnEvaluationResponse> calculatedConfigs = new ArrayList<>();
    private final Map<String, Object> taskAttributes = new HashMap<>();
    ConfigurationDmnEvaluationResponse dueDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("dueDate"))
        .value(CamundaValue.stringValue("2023-01-10T16:00"))
        .build();
    ConfigurationDmnEvaluationResponse priorityDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("priorityDate"))
        .value(CamundaValue.stringValue("2023-01-10T16:00"))
        .build();
    ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
        .name(CamundaValue.stringValue("nextHearingDate"))
        .value(CamundaValue.stringValue("2023-01-10T16:00"))
        .build();
    @Mock
    private DateCalculator dueDateCalculator;
    @Mock
    private DateCalculator priorityDateCalculator;
    @Mock
    private DateCalculator nextHearingDateCalculator;
    @Mock
    private DateCalculator intermediateDateCalculator;
    private DateTypeConfigurator dateTypeConfigurator;

    @BeforeEach
    public void setUp() {
        lenient().when(dueDateCalculator.supports(anyList(), eq(DUE_DATE_TYPE), eq(false))).thenReturn(true);
        lenient().when(dueDateCalculator.supports(anyList(), eq(NEXT_HEARING_DATE_TYPE), eq(false)))
            .thenReturn(false);
        lenient().when(dueDateCalculator.supports(anyList(), eq(INTERMEDIATE_DATE_TYPE), eq(false)))
            .thenReturn(false);
        lenient().when(dueDateCalculator.supports(anyList(), eq(PRIORITY_DATE_TYPE), eq(false))).thenReturn(false);
        lenient().when(priorityDateCalculator.supports(anyList(), eq(PRIORITY_DATE_TYPE), eq(false)))
            .thenReturn(true);
        lenient().when(priorityDateCalculator.supports(anyList(), eq(INTERMEDIATE_DATE_TYPE), eq(false)))
            .thenReturn(false);
        lenient().when(priorityDateCalculator.supports(anyList(), eq(NEXT_HEARING_DATE_TYPE), eq(false)))
            .thenReturn(false);
        lenient().when(nextHearingDateCalculator.supports(anyList(), eq(NEXT_HEARING_DATE_TYPE), eq(false)))
            .thenReturn(true);
        lenient().when(nextHearingDateCalculator.supports(anyList(), eq(INTERMEDIATE_DATE_TYPE), eq(false)))
            .thenReturn(false);
        lenient().when(intermediateDateCalculator.supports(anyList(), eq(INTERMEDIATE_DATE_TYPE), eq(false)))
            .thenReturn(true);

        dateTypeConfigurator = new DateTypeConfigurator(List.of(
            dueDateCalculator,
            priorityDateCalculator,
            nextHearingDateCalculator,
            intermediateDateCalculator
        ));
    }

    @Test
    public void should_use_default_date_calculation_order_when_calculated_date_not_exist() {
        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            dueDate,
            priorityDate,
            nextHearingDate
        );
        dateTypeConfigurator.configureDates(evaluationResponses, false, false, taskAttributes);

        InOrder inOrder = inOrder(dueDateCalculator, priorityDateCalculator, nextHearingDateCalculator);

        inOrder.verify(nextHearingDateCalculator).calculateDate(any(), eq(NEXT_HEARING_DATE_TYPE), eq(false),
                                                                eq(taskAttributes), eq(calculatedConfigs)
        );
        inOrder.verify(dueDateCalculator).calculateDate(any(), eq(DUE_DATE_TYPE), eq(false),
                                                        eq(taskAttributes), eq(calculatedConfigs)
        );
        inOrder.verify(priorityDateCalculator).calculateDate(any(), eq(PRIORITY_DATE_TYPE), eq(false),
                                                             eq(taskAttributes), eq(calculatedConfigs)
        );
    }

    @Test
    public void should_use_date_calculation_order_when_calculated_date_exist() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(
            calculatedDates,
            dueDate,
            priorityDate,
            nextHearingDate
        );
        dateTypeConfigurator.configureDates(evaluationResponses, false, false, taskAttributes);

        InOrder inOrder = inOrder(dueDateCalculator, priorityDateCalculator, nextHearingDateCalculator);

        inOrder.verify(nextHearingDateCalculator).calculateDate(any(), eq(NEXT_HEARING_DATE_TYPE), eq(false),
                                                                eq(taskAttributes), eq(calculatedConfigs)
        );
        inOrder.verify(dueDateCalculator)
            .calculateDate(any(), eq(DUE_DATE_TYPE), eq(false), eq(taskAttributes), eq(calculatedConfigs));
        inOrder.verify(priorityDateCalculator)
            .calculateDate(any(), eq(PRIORITY_DATE_TYPE), eq(false), eq(taskAttributes), eq(calculatedConfigs));
    }

    @Test
    public void should_use_last_date_calculation_order_when_multiple_calculated_date_exist() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate,priorityDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,nextHearingDuration,dueDate,priorityDate"))
            .build();


        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(calculatedDates, calculatedDates2,
                                                                               dueDate, priorityDate, nextHearingDate
        );
        dateTypeConfigurator.configureDates(evaluationResponses, false, false, taskAttributes);

        InOrder inOrder = inOrder(
            nextHearingDateCalculator,
            intermediateDateCalculator,
            dueDateCalculator,
            priorityDateCalculator
        );

        inOrder.verify(nextHearingDateCalculator).calculateDate(any(), eq(NEXT_HEARING_DATE_TYPE), eq(false),
                                                                eq(taskAttributes), eq(calculatedConfigs)
        );
        inOrder.verify(intermediateDateCalculator).calculateDate(any(), eq(INTERMEDIATE_DATE_TYPE), eq(false),
                                                                 eq(taskAttributes), eq(calculatedConfigs)
        );
        inOrder.verify(dueDateCalculator)
            .calculateDate(any(), eq(DUE_DATE_TYPE), eq(false), eq(taskAttributes), eq(calculatedConfigs));
        inOrder.verify(priorityDateCalculator)
            .calculateDate(any(), eq(PRIORITY_DATE_TYPE), eq(false), eq(taskAttributes), eq(calculatedConfigs));
    }

    @Test
    public void should_error_when_calculated_dates_are_in_mandatory_dates_order() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate,dueDate"))
            .build();

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates),
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(MANDATORY_DATES_NOT_IN_REQUIRED_ORDER_IN_CALCULATED_DATES);
    }

    @Test
    public void should_error_when_calculated_dates_are_misses_some_mandatory_date() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,dueDate"))
            .build();

        assertThatThrownBy(() -> dateTypeConfigurator
            .configureDates(
                List.of(calculatedDates),
                false,
                false,
                taskAttributes
            ))
            .isInstanceOf(DateCalculationException.class)
            .hasMessage(MANDATORY_DATES_NOT_PROVIDED_IN_CALCULATED_DATES);
    }
}
