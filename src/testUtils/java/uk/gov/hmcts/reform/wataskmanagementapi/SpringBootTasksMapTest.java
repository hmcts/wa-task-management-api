package uk.gov.hmcts.reform.wataskmanagementapi;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.WARNING_LIST;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public abstract class SpringBootTasksMapTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT = "task/{task-id}/initiation";


    protected void initiateTaskMap(TestVariables testVariables,
                                   Jurisdiction jurisdiction) {
        Headers headers = getAuthHeadersForJurisdiction(jurisdiction);
        initiateTaskMap(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }


    protected void initiateTaskMap(TestVariables testVariables,
                                   Headers headers) {
        initiateTaskMap(testVariables, headers, null, defaultInitiationAssert(testVariables));
    }

    protected void initiateTaskMap(TestVariables testVariables,
                                   Jurisdiction jurisdiction,
                                   Consumer<Response> assertConsumer) {
        Headers headers = getAuthHeadersForJurisdiction(jurisdiction);
        initiateTaskMap(testVariables, headers, null, assertConsumer);
    }

    protected void initiateTaskMap(TestVariables testVariables,
                                   Headers headers,
                                   Consumer<Response> assertConsumer) {
        initiateTaskMap(testVariables, headers, null, assertConsumer);
    }

    protected void initiateTaskMap(TestVariables testVariables,
                                   Jurisdiction jurisdiction,
                                   Map<String, String> additionalProperties) {
        Headers headers = getAuthHeadersForJurisdiction(jurisdiction);
        initiateTaskMap(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    protected void initiateTaskMap(TestVariables testVariables,
                                   Headers headers,
                                   Map<String, String> additionalProperties) {
        initiateTaskMap(testVariables, headers, additionalProperties, defaultInitiationAssert(testVariables));
    }

    private void initiateTaskMap(TestVariables testVariables,
                                 Headers headers,
                                 Map<String, String> additionalProperties,
                                 Consumer<Response> assertConsumer) {
        log.info("Task initiate with cron job {}", isInitiationJobRunning());
        if (isInitiationJobRunning()) {
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
            sendInitiateRequestMap(testVariables, additionalProperties);
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

    private Headers getAuthHeadersForJurisdiction(Jurisdiction jurisdiction) {
        switch (jurisdiction) {
            case IA:
                return iaCaseworkerCredentials.getHeaders();
            case WA:
                return waCaseworkerCredentials.getHeaders();
            default:
                return null;
        }
    }

    private void sendInitiateRequestMap(TestVariables testVariables, Map<String, String> additionalProperties) {
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

        taskAttributes.putAll(additionalProperties);

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

        response.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }

    protected String getAssigneeId(Headers headers) {
        return authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }
}
