package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.time.OffsetDateTime;
import java.util.Optional;

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
        } else if ((oldState.equals(CFTTaskState.UNCONFIGURED) || oldState.equals(CFTTaskState.UNASSIGNED))
                   && taskResource.getState().equals(CFTTaskState.ASSIGNED)) {
            return TaskAction.AUTO_ASSIGN;
        } else if (oldState.equals(CFTTaskState.ASSIGNED) && taskResource.getState().equals(CFTTaskState.UNASSIGNED)) {
            return TaskAction.AUTO_UNASSIGN;
        }

        return TaskAction.CONFIGURE;
    }

    public static TaskAction buildTaskActionAttributeForAssign(String assigner, Optional<String> newAssignee,
                                                               Optional<String> oldAssignee) {
        if (isAssign(assigner, newAssignee, oldAssignee)) {
            return TaskAction.ASSIGN;
        } else if (isClaim(assigner, newAssignee, oldAssignee)) {
            return TaskAction.CLAIM;
        } else if (isUnassign(assigner, newAssignee, oldAssignee)) {
            return TaskAction.UNASSIGN;
        } else if (isUnclaim(assigner, newAssignee, oldAssignee)) {
            return TaskAction.UNCLAIM;
        } else if (isUnassignAssign(assigner, newAssignee, oldAssignee)) {
            return TaskAction.UNASSIGN_ASSIGN;
        } else if (isUnassignClaim(assigner, newAssignee, oldAssignee)) {
            return TaskAction.UNASSIGN_CLAIM;
        } else if (isUnclaimAssign(assigner, newAssignee, oldAssignee)) {
            return TaskAction.UNCLAIM_ASSIGN;
        }
        return null;
    }

    public static boolean isAssign(String assigner, Optional<String> newAssignee, Optional<String> oldAssignee) {
        return oldAssignee.isEmpty() && newAssignee.isPresent()
            && !StringUtils.equals(newAssignee.get(), assigner);
    }

    public static boolean isClaim(String assigner, Optional<String> newAssignee, Optional<String> oldAssignee) {
        return oldAssignee.isEmpty() && newAssignee.isPresent()
               && StringUtils.equals(newAssignee.get(), assigner);
    }

    public static boolean isUnassign(String assigner, Optional<String> newAssignee, Optional<String> oldAssignee) {
        return newAssignee.isEmpty() && oldAssignee.isPresent()
                   && !StringUtils.equals(oldAssignee.get(), assigner);
    }

    public static boolean isUnclaim(String assigner, Optional<String> newAssignee, Optional<String> oldAssignee) {
        return newAssignee.isEmpty() && oldAssignee.isPresent()
               && StringUtils.equals(oldAssignee.get(), assigner);
    }

    public static boolean isUnassignAssign(String assigner, Optional<String> newAssignee,
                                            Optional<String> oldAssignee) {
        return newAssignee.isPresent() && oldAssignee.isPresent()
               && !oldAssignee.equals(newAssignee)
               && !StringUtils.equals(oldAssignee.get(), assigner)
               && !StringUtils.equals(newAssignee.get(), assigner);
    }

    public static boolean isUnassignClaim(String assigner, Optional<String> newAssignee,
                                           Optional<String> oldAssignee) {
        return newAssignee.isPresent() && oldAssignee.isPresent()
               && !oldAssignee.equals(newAssignee)
               && !StringUtils.equals(oldAssignee.get(), assigner)
               && StringUtils.equals(newAssignee.get(), assigner);
    }

    public static boolean isUnclaimAssign(String assigner, Optional<String> newAssignee,
                                           Optional<String> oldAssignee) {
        return newAssignee.isPresent() && oldAssignee.isPresent()
               && !oldAssignee.equals(newAssignee)
               && !StringUtils.equals(newAssignee.get(), assigner)
               && StringUtils.equals(oldAssignee.get(), assigner);
    }

}
