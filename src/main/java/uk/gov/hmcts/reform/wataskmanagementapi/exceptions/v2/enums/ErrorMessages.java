package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums;

public enum ErrorMessages {

    TASK_UNCLAIM_UNABLE_TO_UPDATE_STATE(
        "Task unclaim failed. "
        + "Unable to update task state to unassigned."),

    TASK_UNCLAIM_UNABLE_TO_UNCLAIM(
        "Task unclaim partially succeeded. "
        + "The Task state was updated to unassigned, but the Task could not be unclaimed."),

    TASK_COMPLETE_UNABLE_TO_UPDATE_STATE(
        "Task complete failed. "
        + "Unable to update task state to completed."),

    TASK_COMPLETE_UNABLE_TO_COMPLETE(
        "Task complete partially succeeded. "
        + "The Task state was updated to completed, but the Task could not be completed."),

    TASK_CLAIM_UNABLE_TO_UPDATE_STATE(
        "Task claim failed. "
        + "Unable to update task state to assigned."),

    TASK_CLAIM_UNABLE_TO_CLAIM(
        "Task claim partially succeeded. "
        + "The Task state was updated to assigned, but the Task could not be claimed."),

    TASK_CANCEL_UNABLE_TO_CANCEL(
        "Unable to cancel the task."),

    TASK_ASSIGN_UNABLE_TO_UPDATE_STATE(
        "Task assign failed. "
        + "Unable to update task state to assigned."),

    TASK_ASSIGN_UNABLE_TO_ASSIGN(
        "Task assign partially succeeded. "
        + "The Task state was updated to assigned, but the Task could not be assigned."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_ASSIGN(
        "Unable to assign the Task to the current user."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_UPDATE_STATE(
        "Task assign and complete partially succeeded. "
        + "The Task was assigned to the user making the request but the Task could not be completed."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_COMPLETE(
        "Task assign and complete partially succeeded. "
        + "The Task was assigned to the user making the request, the task state was also updated to completed, "
        + "but he Task could not be completed."),

    ROLE_ASSIGNMENT_VERIFICATIONS_FAILED(
        "The request failed the Role Assignment checks performed."),

    ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE(
        "The user being assigned the Task has failed the Role Assignment checks performed."),

    ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER(
        "The user assigning the Task has failed the Role Assignment checks performed."),

    GENERIC_FORBIDDEN_ERROR(
        "The action could not be completed because the client/user had insufficient rights to a resource."),

    INITIATE_TASK_PROCESS_ERROR(
        "The action could not be completed because there was a problem when initiating the task."),
    DATABASE_CONFLICT_ERROR(
        "The action could not be completed because there was a conflict in the database."),
    DATABASE_IS_UNAVAILABLE(
        "Database is unavailable."),
    DOWNSTREAM_DEPENDENCY_ERROR(
        "Downstream dependency did not respond as expected and the request could not be completed."),
    TASK_NOT_FOUND_ERROR(
        "The task could not be found."),

    TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED(
        "Task Reconfiguration process failed to mark some or all tasks for a caseId."),

    TASK_RECONFIGURATION_EXECUTE_TASKS_TO_RECONFIGURE_FAILED(
        "Task Reconfiguration process failed to execute reconfiguration for the following tasks:");

    private final String detail;

    ErrorMessages(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

}
