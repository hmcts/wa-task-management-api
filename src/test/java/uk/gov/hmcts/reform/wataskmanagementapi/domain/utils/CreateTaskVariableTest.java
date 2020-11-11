package uk.gov.hmcts.reform.wataskmanagementapi.domain.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskMapper;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

class CreateTaskVariableTest {

    private CamundaTask camundaTask;
    Map<String, CamundaVariable> localVariables;

    private TaskMapper taskMapper;


    @BeforeEach
    void setup() {
        ZonedDateTime created = ZonedDateTime.now();
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

        camundaTask = new CamundaTask("some-id", "some-name", "some-assignee",
                                      created, dueDate, "some-description",
                                      "some-owner", "formKey");


        localVariables = new HashMap<>();
        localVariables.put("taskState", new CamundaVariable(TaskState.ASSIGNED.getTaskState(),"string"));
        localVariables.put("securityClassification", new CamundaVariable("some-classification", "string"));
        localVariables.put("title", new CamundaVariable("some-title", "string"));
        localVariables.put("executionType", new CamundaVariable("some-executionType", "string"));
        localVariables.put("taskSystem", new CamundaVariable("SELF", "string"));
        localVariables.put("jurisdiction", new CamundaVariable("IA", "string"));
        localVariables.put("region", new CamundaVariable("1", "string"));
    }

    @Test
    void should_create_hmcts_objects() {
        CamundaObjectMapper camundaObjectMapper = new CamundaObjectMapper();
        taskMapper = new TaskMapper(camundaObjectMapper);
        Task task = taskMapper.mapToTaskObject(localVariables, camundaTask);

        Assertions.assertThat(task.getId()).isEqualTo("some-id");
        Assertions.assertThat(task.getName()).isEqualTo("some-name");
        Assertions.assertThat(task.getType()).isEqualTo("formKey");
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
        Assertions.assertThat(task.getAutoAssigned()).isFalse();


    }
}
