package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

public class MappedTaskTest {

    ZonedDateTime created = ZonedDateTime.now();
    ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

    @Test
    void should_create_full_object_and_get_values() {

        MappedTask mappedTask = new MappedTask(
            "some-id",
            "some-name",
            "some-type",
            "some-taskState",
            "some-taskSystem",
            "some-security",
            "some-taskTitle",
            created.toString(),
            dueDate.toString(),
            "some-assignee",
            true,
            "some-executionType",
            "some-jurisdiction",
            "some-region",
            "some-location",
            "some-location-name",
            "some-caseTypeId",
            "some-caseId",
            "some-cat",
            "some-case"
        );

        Assertions.assertThat(mappedTask.getId()).isEqualTo("some-id");
        Assertions.assertThat(mappedTask.getName()).isEqualTo("some-name");
        Assertions.assertThat(mappedTask.getType()).isEqualTo("some-type");
        Assertions.assertThat(mappedTask.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(mappedTask.getTaskState()).isEqualTo("some-taskState");
        Assertions.assertThat(mappedTask.getTaskSystem()).isEqualTo("some-taskSystem");
        Assertions.assertThat(mappedTask.getSecurityClassification()).isEqualTo("some-security");
        Assertions.assertThat(mappedTask.getTaskTitle()).isEqualTo("some-taskTitle");
        Assertions.assertThat(mappedTask.getDueDate()).isEqualTo(dueDate.toString());
        Assertions.assertThat(mappedTask.getCreatedDate()).isEqualTo(created.toString());
        Assertions.assertThat(mappedTask.getLocation()).isEqualTo("some-location");
        Assertions.assertThat(mappedTask.getLocationName()).isEqualTo("some-location-name");
        Assertions.assertThat(mappedTask.getExecutionType()).isEqualTo("some-executionType");
        Assertions.assertThat(mappedTask.getJurisdiction()).isEqualTo("some-jurisdiction");
        Assertions.assertThat(mappedTask.getRegion()).isEqualTo("some-region");
        Assertions.assertThat(mappedTask.getCaseTypeId()).isEqualTo("some-caseTypeId");
        Assertions.assertThat(mappedTask.getCaseId()).isEqualTo("some-caseId");
        Assertions.assertThat(mappedTask.getCaseCategory()).isEqualTo("some-cat");
        Assertions.assertThat(mappedTask.getCaseName()).isEqualTo("some-case");
        Assertions.assertThat(mappedTask.getAutoAssigned()).isTrue();

    }
}
