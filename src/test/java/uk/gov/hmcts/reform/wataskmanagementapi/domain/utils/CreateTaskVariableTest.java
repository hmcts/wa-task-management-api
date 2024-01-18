package uk.gov.hmcts.reform.wataskmanagementapi.domain.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskMapper;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


class CreateTaskVariableTest {

    private CamundaTask camundaTask;
    Map<String, CamundaVariable> localVariables;

    private TaskMapper taskMapper;
    private WarningValues warningValues = new WarningValues(
        Arrays.asList(new Warning("123", "some warning"),
            new Warning("456", "some more warning")));

    @BeforeEach
    void setup() {
        ZonedDateTime created = ZonedDateTime.now();
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        camundaTask = new CamundaTask("some-id", "some-name", "some-assignee",
            created, dueDate, "some-description",
            "some-owner", "formKey", "processInstanceId"
        );

        localVariables = new HashMap<>();
        localVariables.put("taskState", new CamundaVariable(TaskState.ASSIGNED.value(), "string"));
        localVariables.put("securityClassification", new CamundaVariable("some-classification", "string"));
        localVariables.put("title", new CamundaVariable("some-title", "string"));
        localVariables.put("executionType", new CamundaVariable("some-executionType", "string"));
        localVariables.put("taskSystem", new CamundaVariable("SELF", "string"));
        localVariables.put("jurisdiction", new CamundaVariable("IA", "string"));
        localVariables.put("region", new CamundaVariable("1", "string"));
        localVariables.put("hasWarnings", new CamundaVariable(false, "boolean"));
        localVariables.put("taskType", new CamundaVariable("task-type", "string"));
        localVariables.put("warningList", new CamundaVariable(warningValues, "WarningValues"));
        localVariables.put("caseManagementCategory", new CamundaVariable("someCaseManagementCategory", "string"));
    }

    @Test
    void should_create_hmcts_objects() {
        CamundaObjectMapper camundaObjectMapper = new CamundaObjectMapper();
        taskMapper = new TaskMapper(camundaObjectMapper);
        Task task = taskMapper.mapToTaskObject(localVariables, camundaTask);

        Assertions.assertThat(task.getId()).isEqualTo("some-id");
        Assertions.assertThat(task.getName()).isEqualTo("some-name");
        Assertions.assertThat(task.getType()).isEqualTo("task-type");
        Assertions.assertThat(task.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(task.getTaskState()).isEqualTo("assigned");
        Assertions.assertThat(task.getTaskSystem()).isEqualTo("SELF");
        Assertions.assertThat(task.getSecurityClassification()).isEqualTo("some-classification");
        Assertions.assertThat(task.getTaskTitle()).isEqualTo("some-title");
        Assertions.assertThat(task.getDueDate()).isEqualTo(camundaTask.getDue().toString());
        Assertions.assertThat(task.getCreatedDate()).isEqualTo(camundaTask.getCreated().toString());
        Assertions.assertThat(task.getLocation()).isNull();
        Assertions.assertThat(task.getLocationName()).isNull();
        Assertions.assertThat(task.getExecutionType()).isEqualTo("some-executionType");
        Assertions.assertThat(task.getJurisdiction()).isEqualTo("IA");
        Assertions.assertThat(task.getRegion()).isEqualTo("1");
        Assertions.assertThat(task.getCaseTypeId()).isNull();
        Assertions.assertThat(task.getCaseId()).isNull();
        Assertions.assertThat(task.getCaseCategory()).isNull();
        Assertions.assertThat(task.getCaseName()).isNull();
        Assertions.assertThat(task.isAutoAssigned()).isFalse();
        Assertions.assertThat(task.getWarnings()).isFalse();
        Assertions.assertThat(task.getWarningList()).isNotNull();
        Assertions.assertThat(task.getCaseManagementCategory()).isNotNull();

    }

    @Test
    void should_return_string_when_camunda_variable_type_is_string() {

        CamundaVariable camundaVariable = new CamundaVariable(TaskState.ASSIGNED.value(), "string");
        Assertions.assertThat(camundaVariable.getType()).isEqualTo("string");

    }
}
