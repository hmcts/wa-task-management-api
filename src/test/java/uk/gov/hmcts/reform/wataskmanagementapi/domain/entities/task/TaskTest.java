package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;

import static java.util.Collections.singleton;

public class TaskTest {

    ZonedDateTime created = ZonedDateTime.now();
    ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);

    @Test
    void should_create_full_object_and_get_values() {

        Task task = new Task(
            "some-id",
            "some-name",
            "some-type",
            "some-taskState",
            "some-taskSystem",
            "some-security",
            "some-taskTitle",
            created,
            dueDate,
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
            "some-case",
            false,
            new WarningValues(Arrays.asList(
                new Warning("123", "some warning"),
                new Warning("456", "some more warning"))
            ),
            "some-case-management-category",
            "hearing_work",
            new TaskPermissions(new HashSet<>(singleton(PermissionTypes.READ)))
        );

        Assertions.assertThat(task.getId()).isEqualTo("some-id");
        Assertions.assertThat(task.getName()).isEqualTo("some-name");
        Assertions.assertThat(task.getType()).isEqualTo("some-type");
        Assertions.assertThat(task.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(task.getTaskState()).isEqualTo("some-taskState");
        Assertions.assertThat(task.getTaskSystem()).isEqualTo("some-taskSystem");
        Assertions.assertThat(task.getSecurityClassification()).isEqualTo("some-security");
        Assertions.assertThat(task.getTaskTitle()).isEqualTo("some-taskTitle");
        Assertions.assertThat(task.getDueDate()).isEqualTo(dueDate.toString());
        Assertions.assertThat(task.getCreatedDate()).isEqualTo(created.toString());
        Assertions.assertThat(task.getLocation()).isEqualTo("some-location");
        Assertions.assertThat(task.getLocationName()).isEqualTo("some-location-name");
        Assertions.assertThat(task.getExecutionType()).isEqualTo("some-executionType");
        Assertions.assertThat(task.getJurisdiction()).isEqualTo("some-jurisdiction");
        Assertions.assertThat(task.getRegion()).isEqualTo("some-region");
        Assertions.assertThat(task.getCaseTypeId()).isEqualTo("some-caseTypeId");
        Assertions.assertThat(task.getCaseId()).isEqualTo("some-caseId");
        Assertions.assertThat(task.getCaseCategory()).isEqualTo("some-cat");
        Assertions.assertThat(task.getCaseName()).isEqualTo("some-case");
        Assertions.assertThat(task.isAutoAssigned()).isTrue();
        Assertions.assertThat(task.getWarnings()).isFalse();
        Assertions.assertThat(task.getWarningList().getValues().size()).isEqualTo(2);
        Assertions.assertThat(task.getWarningList().getValues().get(0).getWarningCode()).isEqualTo("123");
        Assertions.assertThat(task.getWarningList().getValues().get(0).getWarningText()).isEqualTo("some warning");
        Assertions.assertThat(task.getCaseManagementCategory()).isEqualTo("some-case-management-category");
        Assertions.assertThat(task.getAssignee()).isEqualTo("some-assignee");
        Assertions.assertThat(task.isAutoAssigned()).isTrue();
        Assertions.assertThat(task.getPermissions().getValues()).contains(PermissionTypes.READ);
    }


    @Test
    void should_create_task_and_get_values_when_autoAssigned_is_false_and_permission_own() {

        Task task = new Task(
            "some-id",
            "some-name",
            "some-type",
            "some-taskState",
            "some-taskSystem",
            "some-security",
            "some-taskTitle",
            created,
            dueDate,
            null,
            false,
            "some-executionType",
            "some-jurisdiction",
            "some-region",
            "some-location",
            "some-location-name",
            "some-caseTypeId",
            "some-caseId",
            "some-cat",
            "some-case",
            false,
            new WarningValues(Arrays.asList(
                new Warning("123", "some warning"),
                new Warning("456", "some more warning"))
            ),
            "some-case-management-category",
            "hearing_work",
            new TaskPermissions(new HashSet<>(singleton(PermissionTypes.OWN)))
        );

        Assertions.assertThat(task.isAutoAssigned()).isFalse();
        Assertions.assertThat(task.getAssignee()).isNull();
        Assertions.assertThat(task.getPermissions().getValues()).contains(PermissionTypes.OWN);

    }
}
