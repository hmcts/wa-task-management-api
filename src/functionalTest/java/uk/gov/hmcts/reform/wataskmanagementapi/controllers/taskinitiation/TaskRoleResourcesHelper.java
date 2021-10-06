package uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation;

import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;

public class TaskRoleResourcesHelper {

    @NotNull
    public static TaskRoleResource getExpectedCaseManagerTaskRoleResource() {
        return new TaskRoleResource(
            "case-manager",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            true,
            "LEGAL_OPERATIONS"
        );
    }

    @NotNull
    public static TaskRoleResource getExpectedTribunalCaseWorkerTaskRoleResource() {
        return new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            true,
            "LEGAL_OPERATIONS"
        );
    }

    @NotNull
    public static TaskRoleResource getExpectedTaskSupervisorTaskRoleResource() {
        return new TaskRoleResource(
            "task-supervisor",
            true,
            false,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            null,
            false,
            "LEGAL_OPERATIONS"
        );
    }

}
