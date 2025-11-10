package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WARNING_LIST;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.BASE_CASE_WORDER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_GET_ENDPOINT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_INITIATION_ENDPOINT;

@Component
@Profile("functional")
@Slf4j
@Import(AwaitilityTestConfig.class)
public class TaskFunctionalTestsInitiationUtils {

    @Autowired
    protected TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    protected AuthorizationProvider authorizationProvider;

    @Autowired
    protected TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    protected TestAuthenticationCredentials baseCaseworkerCredentials;

    @PostConstruct
    public void setup() throws IOException {
        baseCaseworkerCredentials = taskFunctionalTestsUserUtils
            .getTestUser(BASE_CASE_WORDER);
    }

    public AtomicReference<String> getTaskId(Object taskName, String filter) {
        AtomicReference<String> response = new AtomicReference<>();
        await()
            .until(
                () -> {
                    Response camundaGetTaskResult = taskFunctionalTestsApiUtils.getCamundaApiActions().get(
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

        Response camundaResult = taskFunctionalTestsApiUtils.getCamundaApiActions().post(
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

    public void initiateTask(TestVariables testVariables) {
        Headers headers = baseCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }


    public void initiateTask(TestVariables testVariables,
                             Headers headers) {
        initiateTask(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }

    public void initiateTask(TestVariables testVariables,
                                Consumer<Response> assertConsumer) {
        Headers headers = baseCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, null, assertConsumer);
    }

    public void initiateTask(TestVariables testVariables,
                                Headers headers,
                                Consumer<Response> assertConsumer) {
        initiateTask(testVariables, headers, null, assertConsumer);
    }

    public void initiateTask(TestVariables testVariables,
                                Map<String, String> additionalProperties) {
        Headers headers = baseCaseworkerCredentials.getHeaders();
        initiateTask(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    public void initiateTask(TestVariables testVariables,
                             Headers headers,
                             Map<String, String> additionalProperties) {
        initiateTask(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    public void initiateTask(TestVariables testVariables,
                              Headers headers,
                              Map<String, String> additionalProperties,
                              Consumer<Response> assertConsumer) {
        sendInitiateRequest(testVariables, additionalProperties,headers);

        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
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

    public void sendInitiateRequest(TestVariables testVariables, Map<String, String> additionalProperties,
                                    Headers headers) {

        InitiateTaskRequestMap initiateTaskRequest = initiateTaskRequestMap(testVariables, additionalProperties);
        AtomicReference<Response> response = new AtomicReference<>();
        await()
            .until(() -> {
                response.set(taskFunctionalTestsApiUtils.getRestApiActions().post(
                    TASK_INITIATION_ENDPOINT,
                    testVariables.getTaskId(),
                    initiateTaskRequest,
                    authorizationProvider.getServiceAuthorizationHeadersOnly()
                ));

                boolean isTextPlain = response.get().getHeader("Content-Type").equals("text/plain");
                boolean isStatus503 = response.get().getStatusCode() == 503;
                boolean isNoAvailableServer = response.get().getBody().asString().contains("no available server");

                return !(isStatus503 && isTextPlain && isNoAvailableServer);
            });

        //Note: Since tasks can be initiated directly by task monitor, we will have database conflicts for
        // second initiation request, so we are by-passing 503 and 201 response statuses.
        assertResponse(response.get(), testVariables.getTaskId(), headers);

    }

    public InitiateTaskRequestMap initiateTaskRequestMap(TestVariables testVariables, Map<String,
        String> additionalProperties) {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(10);
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
        taskAttributes.put(WARNING_LIST.value(), testVariables.getWarnings());
        taskAttributes.put("__processCategory__Protection", true);
        Optional.ofNullable(additionalProperties).ifPresent(taskAttributes::putAll);

        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(
            INITIATION,
            taskAttributes
        );
        return initiateTaskRequest;
    }

    private void assertResponse(Response response, String taskId, Headers headers) {
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

                Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "task/{task-id}",
                    taskId,
                    headers
                );
                result.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and()
                    .body("task.id", equalTo(taskId));
                break;
            case 201:
                log.info("task Initiation got successfully with status, {}", statusCode);
                break;
            default:
                log.info("task Initiation failed with status, {}", statusCode);
                throw new RuntimeException("Invalid status received for task initiation " + statusCode);
        }
    }
}
