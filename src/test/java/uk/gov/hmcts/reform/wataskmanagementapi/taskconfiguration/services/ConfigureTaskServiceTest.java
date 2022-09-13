package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

class ConfigureTaskServiceTest {

    private static final String ASSIGNEE = "assignee1";
    private static final TaskToConfigure TASK_TO_CONFIGURE
        = new TaskToConfigure("taskId", "taskTypeId", "caseId", "taskName");
    private TaskConfigurationCamundaService camundaService;
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private TaskAutoAssignmentService autoAssignmentService;
    private CaseConfigurationProviderService caseConfigurationProviderService;
    private CFTTaskMapper cftTaskMapper;
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @BeforeEach
    void setup() {
        camundaService = mock(TaskConfigurationCamundaService.class);
        taskVariableExtractor = mock(TaskConfigurator.class);
        autoAssignmentService = mock(TaskAutoAssignmentService.class);
        caseConfigurationProviderService = mock(CaseConfigurationProviderService.class);
        cftTaskMapper = mock(CFTTaskMapper.class);
        featureFlagProvider = mock(LaunchDarklyFeatureFlagProvider.class);
        configureTaskService = new ConfigureTaskService(
            camundaService,
            Collections.singletonList(taskVariableExtractor),
            autoAssignmentService,
            caseConfigurationProviderService,
            cftTaskMapper,
            featureFlagProvider
        );

    }

    public static Stream<Arguments> scenarioProvider() {
        return Stream.of(
            Arguments.of(true, TASK_TO_CONFIGURE.toBuilder()
                .taskAttributes(Map.of("taskType", "taskTypeId")).build()),
            Arguments.of(false, TASK_TO_CONFIGURE)
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

        HashMap<String, CamundaValue<?>> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaValue<>(TASK_TO_CONFIGURE.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaValue<>(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            CamundaValue.stringValue("taskTypeId")
        );

        doReturn(processVariables).when(camundaService).getVariables(TASK_TO_CONFIGURE.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
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

        verify(taskVariableExtractor).getConfigurationVariables(TASK_TO_CONFIGURE);
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

        HashMap<String, CamundaValue<?>> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaValue<>(TASK_TO_CONFIGURE.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaValue<>(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            CamundaValue.stringValue("taskTypeId")
        );

        doReturn(processVariables).when(camundaService).getVariables(TASK_TO_CONFIGURE.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
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
    void should_get_configuration_with_assignee() {

        final AutoAssignmentResult autoAssignmentResult =
            new AutoAssignmentResult(
                TaskState.ASSIGNED.value(),
                "assignee1"
            );

        when(autoAssignmentService.getAutoAssignmentVariables(TASK_TO_CONFIGURE))
            .thenReturn(autoAssignmentResult);

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(mappedValues));


        final ConfigureTaskResponse configureTaskResponse =
            configureTaskService.getConfiguration(TASK_TO_CONFIGURE);

        assertNotNull(configureTaskResponse);
        assertEquals(configureTaskResponse.getTaskId(), TASK_TO_CONFIGURE.getId());
        assertEquals(configureTaskResponse.getCaseId(), TASK_TO_CONFIGURE.getCaseId());
        assertEquals(ASSIGNEE, configureTaskResponse.getAssignee());
    }

    @Test
    void should_get_configuration_with_no_assignee() {

        final AutoAssignmentResult result = new AutoAssignmentResult(UNASSIGNED.value(), null);

        when(autoAssignmentService.getAutoAssignmentVariables(TASK_TO_CONFIGURE))
            .thenReturn(result);

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(TASK_TO_CONFIGURE))
            .thenReturn(new TaskConfigurationResults(mappedValues));


        final ConfigureTaskResponse configureTaskResponse =
            configureTaskService.getConfiguration(TASK_TO_CONFIGURE);

        assertNotNull(configureTaskResponse);
        assertEquals(configureTaskResponse.getTaskId(), TASK_TO_CONFIGURE.getId());
        assertEquals(configureTaskResponse.getCaseId(), TASK_TO_CONFIGURE.getCaseId());
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void given_r2_feature_flag_value_when_configure_cft_tasks_requested_then_taskToConfigure_is_as_expected(
        boolean featureFlag,
        TaskToConfigure expectedTaskToConfigure) {

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(expectedTaskToConfigure))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        when(featureFlagProvider.getBooleanValue(eq(FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE), any(), any()))
            .thenReturn(featureFlag);

        when(cftTaskMapper.getTaskAttributes(any(TaskResource.class))).thenReturn(Map.of("taskType", "taskTypeId"));

        TaskResource skeletonMappedTask = mock(TaskResource.class);

        configureTaskService.configureCFTTask(skeletonMappedTask, TASK_TO_CONFIGURE);

        verify(taskVariableExtractor).getConfigurationVariables(eq(expectedTaskToConfigure));
    }
}
