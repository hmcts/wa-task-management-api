package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

public class CamundaHelpers {
    public static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    public static final String IDAM_USER_ID = "IDAM_USER_ID";
    public static final String IDAM_USER_EMAIL = "test@test.com";
    public static final String SECONDARY_IDAM_USER_ID = "SECONDARY_IDAM_USER_ID";
    public static final Map<String, String> ADDITIONAL_PROPERTIES = Map.of(
        "name1",
        "value1",
        "name2",
        "value2",
        "name3",
        "value3"
    );

    protected Task createMockedMappedTask() {
        return new Task(
            "someMappedTaskId",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            false,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            false,
            new WarningValues(Collections.emptyList()),
            "someCaseManagementCategory",
            "hearing_work",
            null,
            new TaskPermissions(new HashSet<>(singleton(PermissionTypes.READ))),
            null,
            "a description",
            ADDITIONAL_PROPERTIES,
            "nextHearingId",
            ZonedDateTime.now(),
            500,
            5000,
            ZonedDateTime.now());
    }

    protected CamundaTask createMockedUnmappedTask() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            IDAM_USER_ID,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    protected CamundaTask createMockedUnmappedTaskWithNoAssignee() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    protected Map<String, CamundaVariable> createMockCamundaVariables() {

        WarningValues warningValues = new WarningValues(
            Arrays.asList(new Warning("123", "some warning"),
                new Warning("456", "some more warning")));

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("SC", "String"));
        variables.put("title", new CamundaVariable("some_title", "String"));
        variables.put("executionType", new CamundaVariable("some_executionType", "String"));
        variables.put("taskSystem", new CamundaVariable("some_taskSystem", "String"));
        variables.put("jurisdiction", new CamundaVariable("some_jurisdiction", "String"));
        variables.put("region", new CamundaVariable("some_region", "String"));
        variables.put("appealType", new CamundaVariable("some_appealType", "String"));
        variables.put("autoAssigned", new CamundaVariable("false", "Boolean"));
        variables.put("assignee", new CamundaVariable("uid", "String"));
        variables.put("hasWarnings", new CamundaVariable("true", "Boolean"));
        variables.put("warningList", new CamundaVariable(warningValues, "WarningValues"));
        return variables;
    }

    protected Map<String, CamundaVariable> createMockCamundaVariablesWithWarning() {
        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                        + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("SC", "String"));
        variables.put("title", new CamundaVariable("some_title", "String"));
        variables.put("executionType", new CamundaVariable("some_executionType", "String"));
        variables.put("taskSystem", new CamundaVariable("some_taskSystem", "String"));
        variables.put("jurisdiction", new CamundaVariable("some_jurisdiction", "String"));
        variables.put("region", new CamundaVariable("some_region", "String"));
        variables.put("appealType", new CamundaVariable("some_appealType", "String"));
        variables.put("autoAssigned", new CamundaVariable("false", "Boolean"));
        variables.put("assignee", new CamundaVariable("uid", "String"));
        variables.put("hasWarnings", new CamundaVariable("true", "Boolean"));
        variables.put("warningList", new CamundaVariable(values, "String"));

        return variables;
    }


    protected List<Map<String, CamundaVariable>> mockTaskCompletionDMNResponse() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        Map<String, CamundaVariable> response = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("reviewTheAppeal", "String")
        );
        dmnResult.add(response);
        return dmnResult;
    }

    protected List<Map<String, CamundaVariable>> mockTaskCompletionDMNResponses() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        Map<String, CamundaVariable> response = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("reviewTheAppeal", "String")
        );
        dmnResult.add(response);

        response = Map.of(
            "completionMode", new CamundaVariable("Auto", "String")
        );
        dmnResult.add(response);
        return dmnResult;
    }

    protected List<Map<String, CamundaVariable>> mockTaskCompletionDMNResponseWithEmptyRow() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        final Map<String, CamundaVariable> completionMode = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("reviewTheAppeal", "String")
        );
        dmnResult.add(completionMode);
        dmnResult.add(emptyMap());

        return dmnResult;
    }

    protected List<CamundaVariableInstance> mockedVariablesResponse(String processInstanceId, String taskId) {
        Map<String, CamundaVariable> mockVariables = createMockCamundaVariables();

        return mockVariables.keySet().stream()
            .map(
                mockVarKey ->
                    new CamundaVariableInstance(
                        mockVariables.get(mockVarKey).getValue(),
                        mockVariables.get(mockVarKey).getType(),
                        mockVarKey,
                        processInstanceId,
                        taskId
                    ))
            .collect(Collectors.toList());

    }

    protected List<CamundaVariableInstance> mockedVariablesResponseWithWarning(String processInstanceId,
                                                                               String taskId) {
        Map<String, CamundaVariable> mockVariables = createMockCamundaVariablesWithWarning();

        return mockVariables.keySet().stream()
            .map(
                mockVarKey ->
                    new CamundaVariableInstance(
                        mockVariables.get(mockVarKey).getValue(),
                        mockVariables.get(mockVarKey).getType(),
                        mockVarKey,
                        processInstanceId,
                        taskId
                    ))
            .collect(Collectors.toList());

    }

    protected List<CamundaVariableInstance> mockedVariablesResponseForMultipleProcessIds() {
        List<CamundaVariableInstance> variablesForProcessInstance1 =
            mockedVariablesResponse("someProcessInstanceId", "someTaskId");
        List<CamundaVariableInstance> variablesForProcessInstance2 =
            mockedVariablesResponse("someProcessInstanceId2", "someTaskId2");

        return Stream.of(variablesForProcessInstance1, variablesForProcessInstance2)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

}
