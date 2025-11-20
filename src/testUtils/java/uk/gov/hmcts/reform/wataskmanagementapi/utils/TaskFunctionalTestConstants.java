package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class TaskFunctionalTestConstants {

    private TaskFunctionalTestConstants() {

    }

    public static final String LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_NOT_ASSIGNED =
        "Could not complete task with id: %s as task was not previously assigned";
    public static final String LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_ASSIGNED_TO_OTHER_USER =
        "Could not complete task with id: %s as task was assigned to other user %s";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static final String TASK_INITIATION_ENDPOINT = "task/{task-id}/initiation";
    public static final String TASK_GET_ENDPOINT = "task/{task-id}";
    public static final String TASK_GET_ROLES_ENDPOINT = "task/{task-id}/roles";
    public static final String WA_JURISDICTION = "WA";
    public static final String WA_CASE_TYPE = "WaCaseType";
    public static final String EMAIL_PREFIX_R3_5 = "wa-granular-permission-";
    public static String ROLE_ASSIGNMENT_VERIFICATION_TYPE =
        "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure";
    public static String ROLE_ASSIGNMENT_VERIFICATION_TITLE = "Role Assignment Verification";
    public static String ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED =
        "Role Assignment Verification: The request failed the Role Assignment checks performed.";

    public static final String BASE_CASE_WORDER = "base-case-worker";
    public static final String GIN_INDEX_CASE_WORKER = "gin-index-case-worker";

    public static final String USER_WITH_NO_ROLES = "USER_WITH_NO_ROLES";
    public static final String USER_WITH_CFT_ORG_ROLES = "USER_WITH_CFT_ORG_ROLES";
    public static final String USER_WITH_WA_ORG_ROLES = "USER_WITH_WA_ORG_ROLES";
    public static final String USER_WITH_WA_ORG_ROLES2 = "USER_WITH_WA_ORG_ROLES2";
    public static final String USER_WITH_WA_ORG_ROLES3 = "USER_WITH_WA_ORG_ROLES23";
    public static final String USER_WITH_COMPLETION_ENABLED = "USER_WITH_COMPLETION_ENABLED";
    public static final String USER_WITH_TRIB_ROLE_COMPLETION_ENABLED = "USER_WITH_TRIB_ROLE_COMPLETION_ENABLED";
    public static final String USER_WITH_TRIB_ROLE_COMPLETION_DISABLED = "USER_WITH_TRIB_ROLE_COMPLETION_DISABLED";
    public static final String USER_WITH_COMPLETION_DISABLED = "USER_WITH_COMPLETION_DISABLED";

    public static final String USER_WITH_CANCELLATION_ENABLED = "USER_WITH_COMPLETION_ENABLED";
    public static final String USER_WITH_CANCELLATION_DISABLED = "USER_WITH_COMPLETION_DISABLED";
    public static final String USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES =
        "USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES";
    public static final String USER_WITH_TRIB_CASEWORKER_ROLE = "USER_WITH_TRIB_CASEWORKER_ROLE";
    public static final String CASE_WORKER_WITH_JUDGE_ROLE = "CASE_WORKER_WITH_JUDGE_ROLE";
    public static final String CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS = "CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS";
    public static final String CASE_WORKER_WITH_CASE_MANAGER_ROLE = "CASE_WORKER_WITH_CASE_MANAGER_ROLE";
    public static final String CASE_WORKER_WITH_CASE_MANAGER_ROLE2 = "CASE_WORKER_WITH_CASE_MANAGER_ROLE2";
    public static final String CASE_WORKER_WITH_CFTC_ROLE = "CASE_WORKER_WITH_CFTC_ROLE";
    public static final String CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE = "CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE";
    public static final String GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE = "GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE";
    public static final String CASE_WORKER_WITH_SENIOR_TRIB_ROLE = "CASE_WORKER_WITH_SENIOR_TRIB_ROLE";

    public static final String EMAIL_PREFIX_GIN_INDEX = "wa-gin-index-";

}
