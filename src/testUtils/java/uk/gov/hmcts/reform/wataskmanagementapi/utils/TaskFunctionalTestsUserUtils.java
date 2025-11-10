package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@Component
@Profile("functional")
public class TaskFunctionalTestsUserUtils {

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

    protected static final String WA_JURISDICTION = "WA";
    protected static final String WA_CASE_TYPE = "WaCaseType";
    protected static final String EMAIL_PREFIX_R3_5 = "wa-granular-permission-";

    public static final String EMAIL_PREFIX_GIN_INDEX = "wa-gin-index-";

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    protected AuthorizationProvider authorizationProvider;

    @Getter
    public Header authorizationHeaders;

    protected Map<String,TestAuthenticationCredentials> testUsersMap = new HashMap<>();

    @PostConstruct
    public void setup() {

        authorizationHeaders = authorizationProvider.getCaseworkerAuthorizationOnly("wa-ft-test-");

        TestAuthenticationCredentials baseCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            baseCaseworkerCredentials.getHeaders());
        testUsersMap.put(BASE_CASE_WORDER, baseCaseworkerCredentials);

        testUsersMap.put(
            USER_WITH_NO_ROLES,
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5)
        );

        TestAuthenticationCredentials caseWorkerWithCftOrgRoles =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            caseWorkerWithCftOrgRoles.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        testUsersMap.put(USER_WITH_CFT_ORG_ROLES, caseWorkerWithCftOrgRoles);

        TestAuthenticationCredentials caseWorkerWithWAOrgRoles =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithWAOrgRoles.getHeaders());
        testUsersMap.put(USER_WITH_WA_ORG_ROLES, caseWorkerWithWAOrgRoles);

        TestAuthenticationCredentials caseWorkerWithWAOrgRoles2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithWAOrgRoles2.getHeaders());
        testUsersMap.put(USER_WITH_WA_ORG_ROLES2, caseWorkerWithWAOrgRoles2);

        TestAuthenticationCredentials caseWorkerWithWAOrgRoles3 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithWAOrgRoles3.getHeaders());
        testUsersMap.put(USER_WITH_WA_ORG_ROLES3, caseWorkerWithWAOrgRoles3);

        TestAuthenticationCredentials caseWorkerWithCompletionEnabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-enabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithCompletionEnabled.getHeaders());
        testUsersMap.put(USER_WITH_COMPLETION_ENABLED, caseWorkerWithCompletionEnabled);

        TestAuthenticationCredentials tribCaseworkerWithCompletionEnabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-enabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            tribCaseworkerWithCompletionEnabled.getHeaders(), "tribunal-caseworker");
        testUsersMap.put(USER_WITH_TRIB_ROLE_COMPLETION_ENABLED, tribCaseworkerWithCompletionEnabled);

        TestAuthenticationCredentials caseWorkerWithCompletionDisabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-disabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithCompletionDisabled.getHeaders());
        testUsersMap.put(USER_WITH_COMPLETION_DISABLED, caseWorkerWithCompletionDisabled);

        TestAuthenticationCredentials tribCaseworkerWithCompletionDisabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-disabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            tribCaseworkerWithCompletionDisabled.getHeaders(), "tribunal-caseworker");
        testUsersMap.put(USER_WITH_TRIB_ROLE_COMPLETION_DISABLED, tribCaseworkerWithCompletionDisabled);

        TestAuthenticationCredentials caseWorkerWithTribRoleWithWorkTypes =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithWorkTypes(
            caseWorkerWithTribRoleWithWorkTypes.getHeaders(), "tribunal-caseworker");
        testUsersMap.put(USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES, caseWorkerWithTribRoleWithWorkTypes);

        TestAuthenticationCredentials caseWorkerWithTribRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithTribRole.getHeaders(), "tribunal-caseworker");
        testUsersMap.put(USER_WITH_TRIB_CASEWORKER_ROLE, caseWorkerWithTribRole);

        TestAuthenticationCredentials caseWorkerWithJudgeRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithJudgeRole.getHeaders(), "judge");
        testUsersMap.put(CASE_WORKER_WITH_JUDGE_ROLE, caseWorkerWithJudgeRole);

        TestAuthenticationCredentials hearingPanelJudgeForStandardAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForStandardAccess(
            hearingPanelJudgeForStandardAccess.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        testUsersMap.put(CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS, hearingPanelJudgeForStandardAccess);

        TestAuthenticationCredentials userWithCaseManagerRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon()
            .setupWAOrganisationalRoleAssignment(userWithCaseManagerRole.getHeaders(), "case-manager");
        testUsersMap.put(CASE_WORKER_WITH_CASE_MANAGER_ROLE, userWithCaseManagerRole);

        TestAuthenticationCredentials userWithCFTCtscRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupCFTCtscRoleAssignmentForWA(userWithCFTCtscRole.getHeaders());
        testUsersMap.put(CASE_WORKER_WITH_CFTC_ROLE, userWithCFTCtscRole);

        TestAuthenticationCredentials userWithCaseManagerRole2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon()
            .setupWAOrganisationalRoleAssignment(userWithCaseManagerRole2.getHeaders(), "case-manager");
        testUsersMap.put(CASE_WORKER_WITH_CASE_MANAGER_ROLE2, userWithCaseManagerRole2);

        TestAuthenticationCredentials userWithTaskSupervisorRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon()
            .setupWAOrganisationalRoleAssignment(userWithTaskSupervisorRole.getHeaders(), "task-supervisor");
        testUsersMap.put(CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE, userWithTaskSupervisorRole);

        TestAuthenticationCredentials ginIndexCaseWorker =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon()
            .setupWAOrganisationalRoleAssignment(ginIndexCaseWorker.getHeaders(), "tribunal-caseworker");
        testUsersMap.put(GIN_INDEX_CASE_WORKER, ginIndexCaseWorker);

        TestAuthenticationCredentials ginIndexCaseWorkerWithJudgeRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForStandardAccess(
            ginIndexCaseWorkerWithJudgeRole.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        testUsersMap.put(GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE, ginIndexCaseWorkerWithJudgeRole);

        TestAuthenticationCredentials userWithSeniorTribCaseworker =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon()
            .setupWAOrganisationalRoleAssignment(
                userWithSeniorTribCaseworker.getHeaders(), "senior-tribunal-caseworker");
        testUsersMap.put(CASE_WORKER_WITH_SENIOR_TRIB_ROLE, userWithSeniorTribCaseworker);

    }

    public TestAuthenticationCredentials getTestUser(String userKey) {
        return testUsersMap.get(userKey);
    }

    public String getAssigneeId(Headers headers) {
        return authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }

    @PreDestroy
    public void cleanupBeforeShutdown() {
        if (!testUsersMap.isEmpty()) {
            testUsersMap.forEach((key, user) -> {
                taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(user.getHeaders());
                authorizationProvider.deleteAccount(user.getAccount().getUsername());
            });
        }
    }
}
