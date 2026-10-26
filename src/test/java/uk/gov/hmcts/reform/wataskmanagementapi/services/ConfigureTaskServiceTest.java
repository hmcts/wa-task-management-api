package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.configurators.TaskConfigurator;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState.CONFIGURED;

@ExtendWith(OutputCaptureExtension.class)
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
    private CaseConfigurationProviderService caseConfigurationProviderService;

    @BeforeEach
    void setup() {
        taskVariableExtractor = mock(TaskConfigurator.class);
        cftTaskMapper = mock(CFTTaskMapper.class);
        caseConfigurationProviderService = mock(CaseConfigurationProviderService.class);
        configureTaskService = new ConfigureTaskService(
            Collections.singletonList(taskVariableExtractor),
            caseConfigurationProviderService,
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

    @Test
    void should_log_safe_reconfiguration_inputs_and_missing_dmn_response(CapturedOutput output) {
        ReflectionTestUtils.setField(configureTaskService, "logReconfigurationDmnDetails", true);

        TaskResource taskResource = mock(TaskResource.class);
        OffsetDateTime hearingDate = OffsetDateTime.parse("2022-12-07T14:00:00Z");
        when(taskResource.getTaskId()).thenReturn("task-123");
        when(taskResource.getCaseId()).thenReturn("case-456");
        when(taskResource.getNextHearingDate()).thenReturn(hearingDate);

        Map<String, Object> input = new HashMap<>();
        input.put("taskType", "endToEndTask");
        input.put("taskSystem", "WA");
        input.put("securityClassification", "PUBLIC");
        input.put("autoAssigned", true);
        input.put("workTypeResource", new WorkTypeResource("hearing_work"));
        input.put("businessContext", Map.of("sensitive", "redacted"));
        input.put("executionTypeCode", new ExecutionTypeResource(null, "label", "description"));
        input.put("assignmentExpiry", hearingDate);
        input.put("notes", List.of("private note"));
        input.put("taskRoleResources", List.of("private role"));
        input.put("terminationReason", "private reason");
        input.put("lastUpdatedUser", "private-user");
        input.put("lastUpdatedAction", "private-action");
        input.put("next_hearing_date", hearingDate);
        input.put("caseId", "case-456");

        TaskConfigurationResults results = new TaskConfigurationResults(Map.of(), null, null);
        when(cftTaskMapper.getTaskAttributes(taskResource)).thenReturn(input);
        when(caseConfigurationProviderService.getCaseRelatedConfiguration("case-456", input, true))
            .thenReturn(results);
        when(cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, results)).thenReturn(taskResource);

        configureTaskService.reconfigureCFTTask(taskResource);

        assertThat(output.getOut())
            .contains("Reconfiguration DMN input: taskId=task-123, decisionTableKey=unavailable")
            .contains("next_hearing_date=2022-12-07T14:00Z")
            .contains("workTypeResource=hearing_work")
            .contains("notes=count=1")
            .contains("taskRoleResources=count=1")
            .contains("terminationReason=present; value redacted")
            .contains("storedNextHearingDateBefore=2022-12-07T14:00Z")
            .contains("Reconfiguration DMN nextHearingDate result: taskId=task-123, response=null")
            .contains("storedNextHearingDateAfter=2022-12-07T14:00Z")
            .doesNotContain("private note", "private reason", "private-user", "private-action", "case-456");
    }

    @Test
    void should_log_absent_hearing_date_row_and_decision_key(CapturedOutput output) {
        ReflectionTestUtils.setField(configureTaskService, "logReconfigurationDmnDetails", true);

        TaskResource taskResource = mock(TaskResource.class);
        when(taskResource.getTaskId()).thenReturn("task-789");
        when(taskResource.getCaseId()).thenReturn("case-789");
        when(taskResource.getJurisdiction()).thenReturn("WA");
        when(taskResource.getCaseTypeId()).thenReturn("WACaseType");
        Map<String, Object> input = Map.of("taskType", "endToEndTask");
        TaskConfigurationResults results = new TaskConfigurationResults(Map.of(), List.of(), List.of());
        when(cftTaskMapper.getTaskAttributes(taskResource)).thenReturn(input);
        when(caseConfigurationProviderService.getCaseRelatedConfiguration("case-789", input, true))
            .thenReturn(results);
        when(cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, results)).thenReturn(taskResource);

        configureTaskService.reconfigureCFTTask(taskResource);

        assertThat(output.getOut())
            .contains("decisionTableKey=wa-task-configuration-wa-wacasetype")
            .contains("Reconfiguration DMN nextHearingDate result: taskId=task-789, row=absent");
    }

    @Test
    void should_log_hearing_date_dmn_values_and_reconfigure_flags(CapturedOutput output) {
        ReflectionTestUtils.setField(configureTaskService, "logReconfigurationDmnDetails", true);

        TaskResource taskResource = mock(TaskResource.class);
        when(taskResource.getTaskId()).thenReturn("task-321");
        when(taskResource.getCaseId()).thenReturn("case-321");
        Map<String, Object> input = Map.of("taskType", "endToEndTask");
        List<ConfigurationDmnEvaluationResponse> dmnRows = List.of(
            new ConfigurationDmnEvaluationResponse(null, null, null),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"), null, null),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"), stringValue("2022-12-07T14:00:00Z"), booleanValue(true))
        );
        TaskConfigurationResults results = new TaskConfigurationResults(Map.of(), dmnRows, List.of());
        when(cftTaskMapper.getTaskAttributes(taskResource)).thenReturn(input);
        when(caseConfigurationProviderService.getCaseRelatedConfiguration("case-321", input, true))
            .thenReturn(results);
        when(cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, results)).thenReturn(taskResource);

        configureTaskService.reconfigureCFTTask(taskResource);

        assertThat(output.getOut())
            .contains("Reconfiguration DMN nextHearingDate result: taskId=task-321, value=null, canReconfigure=null")
            .contains("Reconfiguration DMN nextHearingDate result: taskId=task-321, "
                + "value=2022-12-07T14:00:00Z, canReconfigure=true");
    }
}
