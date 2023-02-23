package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.configurators.TaskConfigurator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState.CONFIGURED;

class ConfigureTaskServiceTest {

    private static final TaskToConfigure TASK_TO_CONFIGURE
        = new TaskToConfigure("taskId", "taskTypeId", "caseId", "taskName",
                              Map.of(
                                  TaskAttributeDefinition.TASK_TYPE.value(), "taskTypeId",
                                  TASK_NAME.value(), "taskName",
                                  TASK_CASE_ID.value(), "caseId",
                                  TASK_TITLE.value(), "A test task"
                              )
    );
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private CFTTaskMapper cftTaskMapper;

    @BeforeEach
    void setup() {
        taskVariableExtractor = mock(TaskConfigurator.class);
        cftTaskMapper = mock(CFTTaskMapper.class);
        configureTaskService = new ConfigureTaskService(
            Collections.singletonList(taskVariableExtractor),
            mock(CaseConfigurationProviderService.class),
            cftTaskMapper
        );

    }

    @Test
    void should_configure_task_as_expected() {

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(mappedValues, null, null));

        when(cftTaskMapper.getTaskAttributes(any(TaskResource.class))).thenReturn(Map.of("taskType", "taskTypeId"));

        TaskResource skeletonMappedTask = mock(TaskResource.class);

        configureTaskService.configureCFTTask(skeletonMappedTask, TASK_TO_CONFIGURE);

        verify(taskVariableExtractor).getConfigurationVariables(TASK_TO_CONFIGURE);
    }

    @Test
    void should_configure_task_as_expected_with_process_variables_and_dmn_response() {

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        List<ConfigurationDmnEvaluationResponse> configDmnResponse = List.of(
            new ConfigurationDmnEvaluationResponse(stringValue("key1"), stringValue("value1")),
            new ConfigurationDmnEvaluationResponse(stringValue("key2"), stringValue("value2")));

        List<PermissionsDmnEvaluationResponse> permissionDmnResponse = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(mappedValues, configDmnResponse, permissionDmnResponse));

        when(cftTaskMapper.getTaskAttributes(any(TaskResource.class))).thenReturn(Map.of("taskType", "taskTypeId"));

        TaskResource skeletonMappedTask = mock(TaskResource.class);

        configureTaskService.configureCFTTask(skeletonMappedTask, TASK_TO_CONFIGURE);

        verify(taskVariableExtractor).getConfigurationVariables(TASK_TO_CONFIGURE);
    }

    @Test
    void should_configure_task_as_expected_with_dmn_response_but_no_process_variables() {

        List<ConfigurationDmnEvaluationResponse> configDmnResponse = List.of(
            new ConfigurationDmnEvaluationResponse(stringValue("key1"), stringValue("value1")),
            new ConfigurationDmnEvaluationResponse(stringValue("key2"), stringValue("value2")));

        List<PermissionsDmnEvaluationResponse> permissionDmnResponse = asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(null, configDmnResponse, permissionDmnResponse));

        when(cftTaskMapper.getTaskAttributes(any(TaskResource.class))).thenReturn(Map.of("taskType", "taskTypeId"));

        TaskResource skeletonMappedTask = mock(TaskResource.class);

        configureTaskService.configureCFTTask(skeletonMappedTask, TASK_TO_CONFIGURE);

        verify(taskVariableExtractor).getConfigurationVariables(TASK_TO_CONFIGURE);
    }
}
