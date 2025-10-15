package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@Component
public class TaskFunctionalTestsUserUtils {

    public static final String CASE_WORKER = "case-worker";
    public static final String WA_CASE_WORKER = "wa-case-worker";
    public static final String ASSIGNER = "assigner";
    public static final String ASSIGNEE = "assignee";
    public static final String SECOND_ASSIGNEE = "second-assignee";
    public static final String CASE_WORKER_FOR_READ = "case-worker-for-read";
    public static final String CURRENT_CASE_WORKER = "current-caseWorker";
    public static final String UNASSIGN_USER = "unassign-user";
    public static final String OTHER_USER = "other-user";
    public static final String GIN_INDEX_CASE_WORKER = "gin-index-case-worker";
    public static final String WA_USER_COMPLETION_ENABLED = "wa-user-with-completion-process-enabled-";
    public static final String WA_USER_COMPLETION_DISABLED = "wa-user-with-completion-process-disabled-";
    public static final String EMAIL_PREFIX_R2 = "wa-ft-test-r2-";
    public static final String EMAIL_PREFIX_R3 = "wa-ft-test-r3-";

    protected static final String WA_JURISDICTION = "WA";
    protected static final String WA_CASE_TYPE = "WaCaseType";
    protected static final String EMAIL_PREFIX_R3_5 = "wa-granular-permission-";

    private static final String EMAIL_PREFIX_GIN_INDEX = "wa-gin-index-";

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    protected AuthorizationProvider authorizationProvider;

    protected String idamSystemUser;

    protected Map<String,TestAuthenticationCredentials> testUsersMap = new HashMap<>();

    @PostConstruct
    public void setup() {
        testUsersMap.put(CASE_WORKER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R2));
        testUsersMap.put(WA_CASE_WORKER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(ASSIGNER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(ASSIGNEE, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(SECOND_ASSIGNEE, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(CASE_WORKER_FOR_READ, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(CURRENT_CASE_WORKER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(UNASSIGN_USER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(OTHER_USER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5));
        testUsersMap.put(GIN_INDEX_CASE_WORKER, authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX));
        testUsersMap.put(WA_USER_COMPLETION_ENABLED,
                         authorizationProvider.getNewTribunalCaseworker(WA_USER_COMPLETION_ENABLED));
        testUsersMap.put(WA_USER_COMPLETION_DISABLED,
                         authorizationProvider.getNewTribunalCaseworker(WA_USER_COMPLETION_DISABLED));
        testUsersMap.put(EMAIL_PREFIX_R3,
                         authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3));
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
