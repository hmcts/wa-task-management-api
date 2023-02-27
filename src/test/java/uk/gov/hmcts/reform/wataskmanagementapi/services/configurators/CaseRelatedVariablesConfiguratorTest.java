package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;

class CaseRelatedVariablesConfiguratorTest {

    private CaseConfigurationProviderService caseConfigurationProviderService;
    private CaseRelatedVariablesConfigurator caseRelatedVariablesConfigurator;

    @BeforeEach
    void setUp() {
        caseConfigurationProviderService = mock(CaseConfigurationProviderService.class);
        caseRelatedVariablesConfigurator = new CaseRelatedVariablesConfigurator(caseConfigurationProviderService);
    }

    @Test
    void should_throw_exception_when_case_id_is_null() {

        TaskToConfigure testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            null,
            "taskName"
        );

        assertThrows(NullPointerException.class, () -> {
            caseRelatedVariablesConfigurator.getConfigurationVariables(testTaskToConfigure);
        });
    }

    @Test
    void should_get_values_from_map_case_details_service() {
        String caseId = "ccd_id_123";
        String taskTypeId = "taskType";

        Map<String, Object> taskAttributes = Map.of("taskTypeId", taskTypeId);
        TaskToConfigure testTaskToConfigure = new TaskToConfigure(
            "taskId",
            taskTypeId,
            caseId,
            "taskName",
            taskAttributes
        );

        Map<String, Object> expectedValues = Map.of(
            SECURITY_CLASSIFICATION.value(), "PUBLIC",
            JURISDICTION.value(), "IA",
            CASE_TYPE_ID.value(), "Asylum"
        );

        when(caseConfigurationProviderService.getCaseRelatedConfiguration(caseId, taskAttributes, false))
            .thenReturn(new TaskConfigurationResults(expectedValues));

        TaskConfigurationResults actual = caseRelatedVariablesConfigurator
            .getConfigurationVariables(testTaskToConfigure);

        assertThat(actual.getProcessVariables(), is(expectedValues));
    }

    @Test
    void should_get_values_from_map_case_details_service_when_taskAttributes_are_null() {
        String caseId = "ccd_id_123";
        String taskTypeId = "taskType";

        TaskToConfigure testTaskToConfigure = new TaskToConfigure(
            "taskId",
            taskTypeId,
            caseId,
            "taskName"
        );

        Map<String, Object> expectedValues = Map.of(
            SECURITY_CLASSIFICATION.value(), "PUBLIC",
            JURISDICTION.value(), "IA",
            CASE_TYPE_ID.value(), "Asylum"
        );

        when(caseConfigurationProviderService.getCaseRelatedConfiguration(caseId, null, false))
            .thenReturn(new TaskConfigurationResults(expectedValues));

        TaskConfigurationResults actual = caseRelatedVariablesConfigurator
            .getConfigurationVariables(testTaskToConfigure);

        assertThat(actual.getProcessVariables(), is(expectedValues));
    }
}
