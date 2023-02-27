package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaskActionAttributeBuilderTest {

    @ParameterizedTest
    @CsvSource({
        "NewAssignee, ASSIGNED, NewAssignee, ASSIGNED, Configure",
        "NewAssignee, ASSIGNED, , UNCONFIGURED, AutoAssign",
        "NewAssignee, ASSIGNED, , UNASSIGNED, AutoAssign",
        "NewAssignee, ASSIGNED, OldAssignee, ASSIGNED, AutoUnassignAssign",
        ", UNASSIGNED, , UNASSIGNED, Configure",
        ", UNASSIGNED, OldAssignee, ASSIGNED, AutoUnassign"
    })
    void should_build_task_action_correctly_when_task_is_auto_assigned(String newAssignee, String newCFTState,
                                                                       String oldAssignee, String oldCftState,
                                                                       String taskAction) {
        TaskResource resource = new TaskResource("taskId",
            "taskName",
            "taskType",
            CFTTaskState.from(newCFTState).get());
        resource.setAssignee(newAssignee);
        TaskAction action = TaskActionAttributesBuilder.buildTaskActionAttribute(
            resource,
            CFTTaskState.from(oldCftState).get(),
            oldAssignee);
        assertEquals(action, TaskAction.from(taskAction).get());
    }

    @ParameterizedTest
    @CsvSource({
        "newAssignee, , Assigner, Assign",
        "assigner, , assigner, Claim",
        ", oldAssignee, assigner, Unassign",
        ", assigner, assigner, Unclaim",
        "newAssignee, oldAssignee, assigner, UnassignAssign",
        "assigner, oldAssignee, assigner, UnassignClaim",
        "newAssignee, assigner, assigner, UnclaimAssign",
        "newAssignee, newAssignee, assigner, ",
        ", , Assigner, ",
    })
    void should_build_task_action_correctly_when_task_is_assigned(String newAssignee, String oldAssignee,
                                                                  String assigner, String taskAction) {
        TaskAction action = TaskActionAttributesBuilder.buildTaskActionAttributeForAssign(
            assigner,
            Optional.ofNullable(newAssignee),
            Optional.ofNullable(oldAssignee));

        if (taskAction != null) {
            assertEquals(action, TaskAction.from(taskAction).get());
            assertEquals(taskAction.equals("Assign"), TaskActionAttributesBuilder.isAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("Claim"), TaskActionAttributesBuilder.isClaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("Unassign"), TaskActionAttributesBuilder.isUnassign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("Unclaim"), TaskActionAttributesBuilder.isUnclaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("UnassignAssign"), TaskActionAttributesBuilder.isUnassignAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("UnassignClaim"), TaskActionAttributesBuilder.isUnassignClaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertEquals(taskAction.equals("UnclaimAssign"), TaskActionAttributesBuilder.isUnclaimAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
        } else {
            assertNull(action);
            assertFalse(TaskActionAttributesBuilder.isAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isClaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isUnassign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isUnclaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isUnassignAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isUnassignClaim(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
            assertFalse(TaskActionAttributesBuilder.isUnclaimAssign(assigner,
                Optional.ofNullable(newAssignee),
                Optional.ofNullable(oldAssignee)));
        }
    }
}
