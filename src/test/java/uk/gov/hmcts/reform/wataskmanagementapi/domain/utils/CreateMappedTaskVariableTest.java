package uk.gov.hmcts.reform.wataskmanagementapi.domain.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.MappedTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.CreateHmctsTaskVariable;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

class CreateMappedTaskVariableTest {

    private CamundaTask camundaTask;
    Map<String, CamundaVariable> localVariables;

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
    void should_create_hmcts_object() {
        MappedTask mappedTask = new CreateHmctsTaskVariable().addVariables(localVariables, camundaTask);

        Assertions.assertThat(mappedTask.getId()).isEqualTo("some-id");
        Assertions.assertThat(mappedTask.getName()).isEqualTo("some-name");
        Assertions.assertThat(mappedTask.getType()).isEqualTo("formKey");
        Assertions.assertThat(mappedTask.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(mappedTask.getTaskState()).isEqualTo("assigned");
        Assertions.assertThat(mappedTask.getTaskSystem()).isEqualTo("SELF");
        Assertions.assertThat(mappedTask.getSecurityClassification()).isEqualTo("some-classification");
        Assertions.assertThat(mappedTask.getTaskTitle()).isEqualTo("some-title");
        Assertions.assertThat(mappedTask.getDueDate()).isEqualTo(camundaTask.getDue().toString());
        Assertions.assertThat(mappedTask.getCreatedDate()).isEqualTo(camundaTask.getCreated().toString());
        Assertions.assertThat(mappedTask.getLocation()).isNull();
        Assertions.assertThat(mappedTask.getLocationName()).isNull();
        Assertions.assertThat(mappedTask.getExecutionType()).isEqualTo("some-executionType");
        Assertions.assertThat(mappedTask.getJurisdiction()).isEqualTo("IA");
        Assertions.assertThat(mappedTask.getRegion()).isEqualTo("1");
        Assertions.assertThat(mappedTask.getCaseTypeId()).isNull();
        Assertions.assertThat(mappedTask.getCaseId()).isNull();
        Assertions.assertThat(mappedTask.getCaseCategory()).isNull();
        Assertions.assertThat(mappedTask.getCaseName()).isNull();
        Assertions.assertThat(mappedTask.getAutoAssigned()).isFalse();


    }
}
