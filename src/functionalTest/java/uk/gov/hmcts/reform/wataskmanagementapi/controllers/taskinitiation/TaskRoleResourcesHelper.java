package uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation;

import org.jetbrains.annotations.NotNull;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;

public class TaskRoleResourcesHelper {

    private TaskRoleResourcesHelper() {
        //checkstyle
    }

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

    @NotNull
    public static TaskRoleResource getExpectedNationalBusinessCentreTaskRoleResource() {
        return new TaskRoleResource(
            "national-business-centre",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            false,
            "ADMINISTRATOR"
        );
    }

    @NotNull
    public static TaskRoleResource getExpectedHearingCentreAdminTaskRoleResource() {
        return new TaskRoleResource(
            "hearing-centre-admin",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            false,
            "ADMINISTRATOR"
        );
    }

    @NotNull
    public static TaskRoleResource getExpectedHearingJudgeTaskRoleResource() {
        return new TaskRoleResource(
            "hearing-judge",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            true,
            "JUDICIAL"
        );
    }

    @NotNull
    public static TaskRoleResource getExpectedJudgeTaskRoleResource() {
        return new TaskRoleResource(
            "judge",
            true,
            true,
            false,
            false,
            false,
            true,
            new String[]{"IA"},
            null,
            false,
            "JUDICIAL"
        );
    }


}
