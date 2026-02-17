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
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ASSIGNEE_CASE_WORKER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ASSIGNEE_CASE_WORKER_WITH_INCORRECT_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.BASE_CASE_WORDER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CASE_MANAGER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CASE_MANAGER_ROLE2;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CFTC_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_SENIOR_TRIB_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_GIN_INDEX;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.GIN_INDEX_CASE_WORKER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.MULTI_ASSIGNEE_CASE_WORKER_1;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.MULTI_ASSIGNEE_CASE_WORKER_2;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_CANCELLATION_DISABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_CANCELLATION_ENABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_CFT_ORG_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_COMPLETION_DISABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_COMPLETION_ENABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_NO_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_CASEWORKER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_CASEWORKER_ROLE_WITH_WORKTYPES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_ROLE_COMPLETION_DISABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_ROLE_COMPLETION_ENABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES2;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES3;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@Component
@Profile("functional")
public class TaskFunctionalTestsUserUtils {

    TestAuthenticationCredentials baseCaseworkerCredentials;

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

        baseCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            baseCaseworkerCredentials.getHeaders());

        testUsersMap.put(BASE_CASE_WORDER, baseCaseworkerCredentials);

        testUsersMap.put(USER_WITH_NO_ROLES,
                         authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));

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

        TestAuthenticationCredentials caseWorkerWithCancellationEnabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-cancellation-process-enabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithCancellationEnabled.getHeaders());
        testUsersMap.put(USER_WITH_CANCELLATION_ENABLED, caseWorkerWithCancellationEnabled);

        TestAuthenticationCredentials caseWorkerWithCancellationDisabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-cancellation-process-disabled-");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseWorkerWithCancellationDisabled.getHeaders());
        testUsersMap.put(USER_WITH_CANCELLATION_DISABLED, caseWorkerWithCancellationDisabled);

        TestAuthenticationCredentials assigneeCaseWorker =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmail("taskassignee.test");
        testUsersMap.put(ASSIGNEE_CASE_WORKER, assigneeCaseWorker);

        TestAuthenticationCredentials assigneeCaseWorkerWithIncorrectRoles =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmail("incorrectroletaskassignee.test");
        testUsersMap.put(ASSIGNEE_CASE_WORKER_WITH_INCORRECT_ROLES, assigneeCaseWorkerWithIncorrectRoles);

        TestAuthenticationCredentials multiAssigneeCaseWorker1 =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmail("multipletaskassignee.test1");
        testUsersMap.put(MULTI_ASSIGNEE_CASE_WORKER_1, multiAssigneeCaseWorker1);

        final TestAuthenticationCredentials multiAssigneeCaseWorker2 =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmail("multipletaskassignee.test2");
        testUsersMap.put(MULTI_ASSIGNEE_CASE_WORKER_2, multiAssigneeCaseWorker2);


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
