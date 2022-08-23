package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestNew;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DocumentManagementFiles;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Assertions;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ADDITIONAL_PROPERTIES;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ADDITIONAL_PROPERTIES;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.WARNING_LIST;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public abstract class SpringBootFunctionalBaseTest {

    public static final String LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID =
        "There was a problem fetching the task with id: %s";
    public static final String LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK =
        "There was a problem fetching the variables for task with id: %s";
    public static final String LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_NOT_ASSIGNED =
        "Could not complete task with id: %s as task was not previously assigned";
    public static final DateTimeFormatter CAMUNDA_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    protected static final String TASK_INITIATION_ENDPOINT = "task/{task-id}";
    protected static final String TASK_INITIATION_ENDPOINT_NEW = "task/{task-id}/new";
    protected static final String WA_JURISDICTION = "WA";
    protected static final String WA_CASE_TYPE = "WaCaseType";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_TYPE =
        "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_TITLE = "Role Assignment Verification";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_DETAIL =
        "Role Assignment Verification: "
            + "The user being assigned the Task has failed the Role Assignment checks performed.";
    protected static String ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED =
        "Role Assignment Verification: The request failed the Role Assignment checks performed.";

    protected GivensBuilder given;
    protected Assertions assertions;
    protected Common common;
    protected RestApiActions restApiActions;
    protected RestApiActions camundaApiActions;
    protected RestApiActions launchDarklyActions;
    @Autowired
    protected AuthorizationProvider authorizationProvider;
    @Autowired
    protected CoreCaseDataApi coreCaseDataApi;
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

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.instance}")
    private String testUrl;
    @Value("${launch_darkly.url}")
    private String launchDarklyUrl;

    @Before
    public void setUpGivens() throws IOException {
        restApiActions = new RestApiActions(testUrl, SNAKE_CASE).setUp();
        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        assertions = new Assertions(camundaApiActions, restApiActions, authorizationProvider);

        launchDarklyActions = new RestApiActions(launchDarklyUrl, LOWER_CAMEL_CASE).setUp();
        documentManagementFiles.prepare();

        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            coreCaseDataApi,
            documentManagementFiles
        );

        common = new Common(
            given,
            restApiActions,
            camundaApiActions,
            authorizationProvider,
            idamService,
            roleAssignmentServiceApi
        );
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

    protected void initiateTask(Headers authenticationHeaders, TestVariables testVariables,
                                String taskType, String taskName, String taskTitle) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, taskType),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, taskName),
            new TaskAttribute(TASK_TITLE, taskTitle),
            new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT,
            testVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }

    protected void initiateTask(Headers authenticationHeaders, TestVariables testVariables,
                                String taskType, String taskName, String taskTitle,
                                Map<String, String> additionalProperties) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> taskAttributes = new ArrayList<>();
        taskAttributes.add(new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, taskType));
        taskAttributes.add(new TaskAttribute(TaskAttributeDefinition.TASK_NAME, taskName));
        taskAttributes.add(new TaskAttribute(TASK_TITLE, taskTitle));
        taskAttributes.add(new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()));
        taskAttributes.add(new TaskAttribute(TASK_CREATED, formattedCreatedDate));
        taskAttributes.add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));

        if (additionalProperties != null) {
            taskAttributes.add(new TaskAttribute(TASK_ADDITIONAL_PROPERTIES, additionalProperties));
        }

        InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(INITIATION, taskAttributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT,
            testVariables.getTaskId(),
            initiateTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }


    protected void initiateTaskWithGenericAttributes(Headers authenticationHeaders, TestVariables testVariables,
                                                     String taskType, String taskName, String taskTitle) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", "12345678",
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );

        WarningValues warningValues = new WarningValues(
            asList(
                new Warning("Code1", "Text1"),
                new Warning("Code2", "Text2")
            ));

        Map<String, Object> taskAttributes = new HashMap<>();
        taskAttributes.put(CamundaVariableDefinition.TASK_TYPE.value(), taskType);
        taskAttributes.put(CamundaVariableDefinition.TASK_NAME.value(), taskName);
        taskAttributes.put(TITLE.value(), taskTitle);
        taskAttributes.put(CASE_ID.value(), testVariables.getCaseId());
        taskAttributes.put(CREATED.value(), formattedCreatedDate);
        taskAttributes.put(DUE_DATE.value(), formattedDueDate);
        taskAttributes.put(ADDITIONAL_PROPERTIES.value(), additionalProperties);
        taskAttributes.put(SECURITY_CLASSIFICATION.value(), SecurityClassification.PUBLIC);
        taskAttributes.put(WARNING_LIST.value(), warningValues);

        InitiateTaskRequestNew req = new InitiateTaskRequestNew(INITIATION, taskAttributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_NEW,
            testVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }

    protected void initiateTaskWithGenericAttributes(Headers authenticationHeaders, TestVariables testVariables,
                                                     String taskType, String taskName, String taskTitle,
                                                     Map<String, String> additionalProperties) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = new HashMap<>();
        taskAttributes.put(CamundaVariableDefinition.TASK_TYPE.value(), taskType);
        taskAttributes.put(CamundaVariableDefinition.TASK_NAME.value(), taskName);
        taskAttributes.put(TITLE.value(), taskTitle);
        taskAttributes.put(CASE_ID.value(), testVariables.getCaseId());
        taskAttributes.put(CREATED.value(), formattedCreatedDate);
        taskAttributes.put(DUE_DATE.value(), formattedDueDate);

        if (additionalProperties != null) {
            taskAttributes.put(ADDITIONAL_PROPERTIES.value(), additionalProperties);
            taskAttributes.put("roleAssignmentId", additionalProperties.get("roleAssignmentId"));
        }

        InitiateTaskRequestNew initiateTaskRequest = new InitiateTaskRequestNew(INITIATION, taskAttributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_NEW,
            testVariables.getTaskId(),
            initiateTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }

    protected String getAssigneeId(Headers headers) {
        return authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }

}
