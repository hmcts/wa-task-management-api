package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CcdRetryableClient;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Assertions;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WARNING_LIST;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public abstract class SpringBootFunctionalBaseTest {

    public static final String LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_NOT_ASSIGNED =
        "Could not complete task with id: %s as task was not previously assigned";
    public static final String LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_ASSIGNED_TO_OTHER_USER =
        "Could not complete task with id: %s as task was assigned to other user %s";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final String TASK_INITIATION_ENDPOINT = "task/{task-id}/initiation";
    protected static final String TASK_GET_ENDPOINT = "task/{task-id}";
    protected static final String TASK_GET_ROLES_ENDPOINT = "task/{task-id}/roles";
    protected static final String WA_JURISDICTION = "WA";
    protected static final String WA_CASE_TYPE = "WaCaseType";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_TYPE =
        "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_TITLE = "Role Assignment Verification";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED =
        "Role Assignment Verification: The request failed the Role Assignment checks performed.";
    protected static String ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER =
        "Role Assignment Verification: The user assigning the Task has failed the Role Assignment checks performed.";
    protected static String TASK_NOT_FOUND_ERROR = "Task Not Found Error: The task could not be found.";
    protected GivensBuilder given;
    protected Assertions assertions;
    protected Common common;
    protected RestApiActions restApiActions;
    protected RestApiActions camundaApiActions;

    protected RestApiActions workflowApiActions;
    protected RestApiActions launchDarklyActions;
    @Autowired
    protected AuthorizationProvider authorizationProvider;

    @Autowired
    protected CcdRetryableClient ccdRetryableClient;
    @Autowired
    protected DocumentManagementFiles documentManagementFiles;
    @Autowired
    protected RoleAssignmentHelper roleAssignmentHelper;
    @Autowired
    protected IdamService idamService;
    @Autowired
    protected RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired
    protected LaunchDarklyFeatureFlagProvider featureFlagProvider;
    @Autowired
    protected IdamTokenGenerator idamTokenGenerator;

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.workflow}")
    private String workflowUrl;
    @Value("${targets.instance}")
    private String testUrl;
    @Value("${launch_darkly.url}")
    private String launchDarklyUrl;
    @Value("${initiation_job_running}")
    private Boolean initiationJobRunning;

    protected TestAuthenticationCredentials waCaseworkerCredentials;
    protected String idamSystemUser;

    @Before
    public void setUpGivens() throws IOException {
        restApiActions = new RestApiActions(testUrl, SNAKE_CASE).setUp();
        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        workflowApiActions = new RestApiActions(workflowUrl, LOWER_CAMEL_CASE).setUp();
        assertions = new Assertions(camundaApiActions, restApiActions, authorizationProvider);

        launchDarklyActions = new RestApiActions(launchDarklyUrl, LOWER_CAMEL_CASE).setUp();
        documentManagementFiles.prepare();

        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            ccdRetryableClient,
            documentManagementFiles,
            workflowApiActions
        );

        common = new Common(
            given,
            restApiActions,
            camundaApiActions,
            authorizationProvider,
            idamService,
            roleAssignmentServiceApi,
            workflowApiActions);

        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        idamSystemUser = idamTokenGenerator.getUserInfo(idamTokenGenerator.generate()).getUid();
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());
    }

    public AtomicReference<String> getTaskId(Object taskName, String filter) {
        AtomicReference<String> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    Response camundaGetTaskResult = camundaApiActions.get(
                        "/task" + filter,
                        authorizationProvider.getServiceAuthorizationHeader()
                    );
                    camundaGetTaskResult.then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("size()", is(1))
                        .body("[0].name", is(taskName));

                    response.set(camundaGetTaskResult
                        .then()
                        .extract()
                        .path("[0].id"));
                    return true;
                });
        return response;
    }

    public String createTask(CreateTaskMessage createTaskMessage) {

        Response camundaResult = camundaApiActions.post(
            "/message",
            createTaskMessage,
            authorizationProvider.getServiceAuthorizationHeader()
        );

        camundaResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Object taskName = createTaskMessage.getProcessVariables().get("name").getValue();

        String filter = "?processVariables=" + "caseId_eq_" + createTaskMessage.getCaseId();

        AtomicReference<String> response = getTaskId(taskName, filter);

        return response.get();

    }

    protected void initiateTask(TestVariables testVariables) {
        Headers headers = waCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }


    protected void initiateTask(TestVariables testVariables,
                                Headers headers) {
        initiateTask(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }

    protected void initiateTask(TestVariables testVariables,
                                Consumer<Response> assertConsumer) {
        Headers headers = waCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, null, assertConsumer);
    }

    protected void initiateTask(TestVariables testVariables,
                                Headers headers,
                                Consumer<Response> assertConsumer) {
        initiateTask(testVariables, headers, null, assertConsumer);
    }

    protected void initiateTask(TestVariables testVariables,
                                Map<String, String> additionalProperties) {
        Headers headers = waCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    protected void initiateTask(TestVariables testVariables,
                                Headers headers,
                                Map<String, String> additionalProperties) {
        initiateTask(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    private void initiateTask(TestVariables testVariables,
                              Headers headers,
                              Map<String, String> additionalProperties,
                              Consumer<Response> assertConsumer) {
        log.info("Task initiate with cron job {}", initiationJobRunning);
        if (initiationJobRunning) {
            await()
                .pollInterval(10, SECONDS)
                .atMost(120, SECONDS)
                .until(
                    () -> {
                        Response response = restApiActions.get(
                            TASK_GET_ENDPOINT,
                            testVariables.getTaskId(),
                            headers
                        );

                        return HttpStatus.OK.value() == response.getStatusCode();
                    }
                );
        } else {
            sendInitiateRequest(testVariables, additionalProperties);
        }

        Response response = restApiActions.get(
            TASK_GET_ENDPOINT,
            testVariables.getTaskId(),
            headers
        );
        assertConsumer.accept(response);
    }

    private Consumer<Response> defaultInitiationAssert(TestVariables testVariables) {
        return (result) -> {
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .contentType(APPLICATION_JSON_VALUE)
                .body("task.id", equalTo(testVariables.getTaskId()))
                .body("task.case_id", equalTo(testVariables.getCaseId()));
        };
    }

    private void sendInitiateRequest(TestVariables testVariables, Map<String, String> additionalProperties) {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        boolean hasWarnings = !testVariables.getWarnings().getValues().isEmpty();

        Map<String, Object> taskAttributes = new HashMap<>();
        taskAttributes.put(CamundaVariableDefinition.TASK_TYPE.value(), testVariables.getTaskType());
        taskAttributes.put(CamundaVariableDefinition.TASK_NAME.value(), testVariables.getTaskName());
        taskAttributes.put(CASE_ID.value(), testVariables.getCaseId());
        taskAttributes.put(CREATED.value(), formattedCreatedDate);
        taskAttributes.put(DUE_DATE.value(), formattedDueDate);
        taskAttributes.put(SECURITY_CLASSIFICATION.value(), SecurityClassification.PUBLIC);
        taskAttributes.put(HAS_WARNINGS.value(), hasWarnings);
        taskAttributes.put(WARNING_LIST.value(), testVariables.getWarnings());

        Optional.ofNullable(additionalProperties).ifPresent(taskAttributes::putAll);

        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(
            INITIATION,
            taskAttributes
        );

        Response response = restApiActions.post(
            TASK_INITIATION_ENDPOINT,
            testVariables.getTaskId(),
            initiateTaskRequest,
            authorizationProvider.getServiceAuthorizationHeadersOnly()
        );

        //Note: Since tasks can be initiated directly by task monitor, we will have database conflicts for
        // second initiation request, so we are by-passing 503 and 201 response statuses.
        assertResponse(response);

    }

    private void assertResponse(Response response) {
        response.prettyPrint();

        int statusCode = response.getStatusCode();
        switch (statusCode) {
            case 503:
                log.info("Initiation failed due to Database Conflict Error, so handling gracefully, {}", statusCode);

                response.then().assertThat()
                    .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .contentType(APPLICATION_PROBLEM_JSON_VALUE)
                    .body("type", equalTo(
                        "https://github.com/hmcts/wa-task-management-api/problem/database-conflict"))
                    .body("title", equalTo("Database Conflict Error"))
                    .body("status", equalTo(503))
                    .body("detail", equalTo(
                        "Database Conflict Error: The action could not be completed because "
                            + "there was a conflict in the database."));
                break;
            case 201:
                log.info("task Initiation got successfully with status, {}", statusCode);
                break;
            default:
                log.info("task Initiation failed with status, {}", statusCode);
                throw new RuntimeException("Invalid status received for task initiation " + statusCode);
        }
    }

    protected String getAssigneeId(Headers headers) {
        return authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }

    protected Boolean isInitiationJobRunning() {
        return this.initiationJobRunning;
    }

}
