package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void should_build_task_action_correctly_when_task_is_assigned(String newAssignee, String newCFTState,
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
}
