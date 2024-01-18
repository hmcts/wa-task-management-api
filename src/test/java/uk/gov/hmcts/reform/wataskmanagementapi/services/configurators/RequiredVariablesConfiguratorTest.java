package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TITLE;

class RequiredVariablesConfiguratorTest {

    private RequiredVariablesConfigurator requiredVariablesConfigurator;

    @BeforeEach
    void setUp() {
        requiredVariablesConfigurator = new RequiredVariablesConfigurator();
    }

    @Test
    void should_add_required_variables() {

        TaskToConfigure testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            "caseId",
            "taskName"
        );

        Map<String, Object> expectedValues = Map.of(
            CASE_ID.value(), "caseId",
            TITLE.value(), "taskName",
            TASK_TYPE.value(), "taskType"
        );

        TaskConfigurationResults actual = requiredVariablesConfigurator.getConfigurationVariables(testTaskToConfigure);

        assertThat(actual.getProcessVariables(), is(expectedValues));
    }

}
