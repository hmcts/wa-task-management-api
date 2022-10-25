package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.configurators.TaskConfigurator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

class ConfigureTaskServiceTest {

    private static final String ASSIGNEE = "assignee1";
    private static final TaskToConfigure TASK_TO_CONFIGURE
        = new TaskToConfigure("taskId", "taskTypeId", "caseId", "taskName",
                              Map.of(
                                  TaskAttributeDefinition.TASK_TYPE.value(), "taskTypeId",
                                  TASK_NAME.value(), "taskName",
                                  TASK_CASE_ID.value(), "caseId",
                                  TASK_TITLE.value(), "A test task"
                              )
    );
    private CamundaService camundaService;
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private TaskAutoAssignmentService autoAssignmentService;
    private CaseConfigurationProviderService caseConfigurationProviderService;
    private CFTTaskMapper cftTaskMapper;

    @BeforeEach
    void setup() {
        camundaService = mock(CamundaService.class);
        taskVariableExtractor = mock(TaskConfigurator.class);
        autoAssignmentService = mock(TaskAutoAssignmentService.class);
        caseConfigurationProviderService = mock(CaseConfigurationProviderService.class);
        cftTaskMapper = mock(CFTTaskMapper.class);
        configureTaskService = new ConfigureTaskService(
            camundaService,
            Collections.singletonList(taskVariableExtractor),
            autoAssignmentService,
            caseConfigurationProviderService,
            cftTaskMapper
        );

    }

    @Test
    void can_configure_a_task_with_variables() {

        String processInstanceId = "processInstanceId";

        CamundaTask camundaTask = new CamundaTask(
            TASK_TO_CONFIGURE.getId(),
            processInstanceId,
            TASK_TO_CONFIGURE.getName()
        );
        when(camundaService.getTask(TASK_TO_CONFIGURE.getId())).thenReturn(camundaTask);

        HashMap<String, CamundaVariable> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaVariable(TASK_TO_CONFIGURE.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaVariable(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            new CamundaVariable(TASK_TYPE.value(), "String")
        );

        doReturn(processVariables).when(camundaService).getTaskVariables(TASK_TO_CONFIGURE.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(any()))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        configureTaskService.configureTask(TASK_TO_CONFIGURE.getId());

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("key1", CamundaValue.stringValue("value1"));
        modifications.put("key2", CamundaValue.stringValue("value2"));
        modifications.put(TASK_TYPE.value(), CamundaValue.stringValue("taskTypeId"));
        modifications.put(TASK_STATE.value(), CamundaValue.stringValue(CONFIGURED.value()));

        verify(camundaService).addProcessVariables(
            TASK_TO_CONFIGURE.getId(),
            modifications
        );

        verify(taskVariableExtractor).getConfigurationVariables(any());
    }

    @Test
    void can_configure_a_task_with_dmn_response() {

        String processInstanceId = "processInstanceId";

        CamundaTask camundaTask = new CamundaTask(
            TASK_TO_CONFIGURE.getId(),
            processInstanceId,
            TASK_TO_CONFIGURE.getName()
        );
        when(camundaService.getTask(TASK_TO_CONFIGURE.getId())).thenReturn(camundaTask);

        HashMap<String, CamundaVariable> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaVariable(TASK_TO_CONFIGURE.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaVariable(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            new CamundaVariable(TASK_TYPE.value(), "String")
        );

        doReturn(processVariables).when(camundaService).getTaskVariables(TASK_TO_CONFIGURE.getId());

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
        when(taskVariableExtractor.getConfigurationVariables(any()))
            .thenReturn(new TaskConfigurationResults(mappedValues, configDmnResponse, permissionDmnResponse));

        configureTaskService.configureTask(TASK_TO_CONFIGURE.getId());

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("key1", CamundaValue.stringValue("value1"));
        modifications.put("key2", CamundaValue.stringValue("value2"));
        modifications.put(TASK_TYPE.value(), CamundaValue.stringValue("taskTypeId"));
        modifications.put(TASK_STATE.value(), CamundaValue.stringValue(CONFIGURED.value()));

        verify(camundaService).addProcessVariables(
            TASK_TO_CONFIGURE.getId(),
            modifications
        );

        verify(taskVariableExtractor).getConfigurationVariables(any());
    }

    @Test
    void can_configure_a_task_with_no_extra_variables() {

        String processInstanceId = "processInstanceId";

        CamundaTask camundaTask = new CamundaTask(
            TASK_TO_CONFIGURE.getId(),
            processInstanceId,
            TASK_TO_CONFIGURE.getName()
        );
        when(camundaService.getTask(TASK_TO_CONFIGURE.getId())).thenReturn(camundaTask);

        HashMap<String, CamundaVariable> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaVariable(TASK_TO_CONFIGURE.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaVariable(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            new CamundaVariable(TASK_TYPE.value(), "String")
        );

        doReturn(processVariables).when(camundaService).getTaskVariables(TASK_TO_CONFIGURE.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");

        when(taskVariableExtractor.getConfigurationVariables(any()))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        configureTaskService.configureTask(TASK_TO_CONFIGURE.getId());

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put(TASK_STATE.value(), CamundaValue.stringValue(CONFIGURED.value()));
        modifications.put(TASK_TYPE.value(), CamundaValue.stringValue("taskTypeId"));

        verify(camundaService).addProcessVariables(
            TASK_TO_CONFIGURE.getId(),
            modifications
        );
    }

    @Test
    void try_to_configure_a_task_that_does_not_exist() {
        String taskIdThatDoesNotExist = "doesNotExist";
        when(camundaService.getTask(taskIdThatDoesNotExist))
            .thenThrow(new ResourceNotFoundException("exception message", new Exception()));

        assertThatThrownBy(() -> configureTaskService.configureTask(taskIdThatDoesNotExist))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("exception message");
    }

    @Test
    void should_configure_task_as_expected() {

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        when(cftTaskMapper.getTaskAttributes(any(TaskResource.class))).thenReturn(Map.of("taskType", "taskTypeId"));

        TaskResource skeletonMappedTask = mock(TaskResource.class);

        configureTaskService.configureCFTTask(skeletonMappedTask, TASK_TO_CONFIGURE);

        verify(taskVariableExtractor).getConfigurationVariables(eq(TASK_TO_CONFIGURE));
    }
}
