package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

import java.time.ZonedDateTime;

public class CamundaTaskTest {

    @Test
    void should_create_minimum_object_and_get_value() {
        CamundaTask camundaTask = new CamundaTask("some-id");
        Assertions.assertThat(camundaTask.getId()).isEqualTo("some-id");
    }

    @Test
    void should_create_full_object_and_get_values() {


        ZonedDateTime created = ZonedDateTime.now();
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        CamundaTask camundaTask = new CamundaTask(
            "some-id",
            "some-name",
            "some-assignee",
            created,
            dueDate,
            "some-description",
            "some-owner"
        );

        Assertions.assertThat(camundaTask.getId()).isEqualTo("some-id");
        Assertions.assertThat(camundaTask.getName()).isEqualTo("some-name");
        Assertions.assertThat(camundaTask.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(camundaTask.getCreated()).isEqualTo(created);
        Assertions.assertThat(camundaTask.getDue()).isEqualTo(dueDate);
        Assertions.assertThat(camundaTask.getDescription()).isEqualTo("some-description");
        Assertions.assertThat(camundaTask.getOwner()).isEqualTo("some-owner");

    }

}
