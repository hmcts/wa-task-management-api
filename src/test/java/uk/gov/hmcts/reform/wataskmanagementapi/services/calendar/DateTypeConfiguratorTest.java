package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@ExtendWith(MockitoExtension.class)
public class DateTypeConfiguratorTest {
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
    private DateTypeConfigurator dateTypeConfigurator;

    @BeforeEach
    public void setUp() {
        when(dueDateCalculator.supports(anyList(), eq(DateType.DUE_DATE), eq(false))).thenReturn(true);
        when(dueDateCalculator.supports(anyList(), eq(DateType.NEXT_HEARING_DATE), eq(false))).thenReturn(false);
        when(dueDateCalculator.supports(anyList(), eq(DateType.PRIORITY_DATE), eq(false))).thenReturn(false);
        when(priorityDateCalculator.supports(anyList(), eq(DateType.PRIORITY_DATE), eq(false))).thenReturn(true);
        when(priorityDateCalculator.supports(anyList(), eq(DateType.NEXT_HEARING_DATE), eq(false))).thenReturn(false);
        when(nextHearingDateCalculator.supports(anyList(), eq(DateType.NEXT_HEARING_DATE), eq(false))).thenReturn(true);

        dateTypeConfigurator = new DateTypeConfigurator(List.of(dueDateCalculator, priorityDateCalculator,
                                                                nextHearingDateCalculator
        ));
    }

    @Test
    public void should_use_default_date_calculation_order_when_calculated_date_not_exist() {
        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(dueDate, priorityDate, nextHearingDate);
        dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        InOrder inOrder = inOrder(dueDateCalculator, priorityDateCalculator, nextHearingDateCalculator);

        inOrder.verify(nextHearingDateCalculator).calculateDate(eq(DateType.NEXT_HEARING_DATE), any());
        inOrder.verify(dueDateCalculator).calculateDate(eq(DUE_DATE), any());
        inOrder.verify(priorityDateCalculator).calculateDate(eq(DateType.PRIORITY_DATE), any());
    }

    @Test
    public void should_use_date_calculation_order_when_calculated_date_exist() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("dueDate,priorityDate,nextHearingDate"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(calculatedDates,
                                                                               dueDate, priorityDate, nextHearingDate
        );
        dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        InOrder inOrder = inOrder(dueDateCalculator, priorityDateCalculator, nextHearingDateCalculator);

        inOrder.verify(dueDateCalculator).calculateDate(eq(DUE_DATE), any());
        inOrder.verify(priorityDateCalculator).calculateDate(eq(DateType.PRIORITY_DATE), any());
        inOrder.verify(nextHearingDateCalculator).calculateDate(eq(DateType.NEXT_HEARING_DATE), any());
    }

    @Test
    public void should_use_last_date_calculation_order_when_multiple_calculated_date_exist() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("dueDate,priorityDate,nextHearingDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates2 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("nextHearingDate,priorityDate,dueDate"))
            .build();

        ConfigurationDmnEvaluationResponse calculatedDates3 = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("calculatedDates"))
            .value(CamundaValue.stringValue("priorityDate,dueDate,nextHearingDate"))
            .build();

        List<ConfigurationDmnEvaluationResponse> evaluationResponses = List.of(calculatedDates, calculatedDates2,
                                                                               calculatedDates3, dueDate,
                                                                               priorityDate, nextHearingDate
        );
        dateTypeConfigurator.configureDates(evaluationResponses, false, false);

        InOrder inOrder = inOrder(dueDateCalculator, priorityDateCalculator, nextHearingDateCalculator);

        inOrder.verify(priorityDateCalculator).calculateDate(eq(DateType.PRIORITY_DATE), any());
        inOrder.verify(dueDateCalculator).calculateDate(eq(DUE_DATE), any());
        inOrder.verify(nextHearingDateCalculator).calculateDate(eq(DateType.NEXT_HEARING_DATE), any());
    }
}
