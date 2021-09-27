package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;

class ConstantVariablesConfiguratorTest {

    private ConstantVariablesConfigurator constantVariablesConfigurator;

    @BeforeEach
    void setUp() {
        constantVariablesConfigurator = new ConstantVariablesConfigurator();
    }

    @Test
    void should_add_constant_variables() {

        TaskToConfigure testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            "caseId",
            "taskName"
        );

        Map<String, Object> expectedValues = Map.of(
            TASK_STATE.value(), CONFIGURED.value(),
            AUTO_ASSIGNED.value(), false,
            EXECUTION_TYPE.value(), "Case Management Task",
            TASK_SYSTEM.value(), "SELF"
        );

        TaskConfigurationResults actual = constantVariablesConfigurator.getConfigurationVariables(testTaskToConfigure);

        assertThat(actual.getProcessVariables(), is(expectedValues));
    }

}
