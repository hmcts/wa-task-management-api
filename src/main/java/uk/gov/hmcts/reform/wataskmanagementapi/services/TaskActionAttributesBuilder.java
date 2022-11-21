package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.time.OffsetDateTime;

public final class TaskActionAttributesBuilder {

    private TaskActionAttributesBuilder() {
    }

    public static void setTaskActionAttributes(TaskResource task, String userId, TaskAction action) {
        task.setLastUpdatedTimestamp(OffsetDateTime.now());
        task.setLastUpdatedUser(userId);
        task.setLastUpdatedAction(action.getValue());
    }

    public static TaskAction buildTaskActionAttribute(TaskResource taskResource, CFTTaskState oldState,
                                                      String oldAssignee) {
        if (oldState.equals(CFTTaskState.ASSIGNED) && taskResource.getState().equals(CFTTaskState.ASSIGNED)
            && !StringUtils.equals(oldAssignee, taskResource.getAssignee())) {
            return TaskAction.AUTO_UNASSIGN_ASSIGN;
        } else if (oldState.equals(CFTTaskState.UNASSIGNED) && taskResource.getState().equals(CFTTaskState.ASSIGNED)) {
            return TaskAction.AUTO_ASSIGN;
        } else if (oldState.equals(CFTTaskState.ASSIGNED) && taskResource.getState().equals(CFTTaskState.UNASSIGNED)) {
            return TaskAction.AUTO_UNASSIGN;
        }

        return TaskAction.CONFIGURE;
    }
}
