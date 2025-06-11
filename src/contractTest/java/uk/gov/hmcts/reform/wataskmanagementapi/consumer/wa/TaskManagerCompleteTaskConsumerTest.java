package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "wa_task_management_api_complete_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerCompleteTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_COMPLETE_TASK_BY_ID = WA_URL + "/" + TASK_ID + "/" + "complete";
    public static final String REQ_PARAM_COMPLETION_PROCESS = "completion_process";

    @Pact(provider = "wa_task_management_api_complete_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeCompleteTaskById204(PactDslWithProvider builder) {

        return builder
            .given("complete a task using taskId")
            .uponReceiving("taskId to complete a task")
            .path(WA_COMPLETE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_complete_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeCompleteTaskById204WithAssignAndComplete(PactDslWithProvider builder) {

        return builder
            .given("complete a task using taskId and assign and complete completion options")
            .uponReceiving("taskId to complete a task")
            .path(WA_COMPLETE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body(createCompleteTaskRequest(), String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_complete_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact pactWithCompletionProcessUser(PactDslWithProvider builder) {
        return buildPactWithQueryParam(builder, "EXUI_USER_COMPLETION");
    }

    @Pact(provider = "wa_task_management_api_complete_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact pactWithCompletionProcessCaseEvent(PactDslWithProvider builder) {
        return buildPactWithQueryParam(builder, "EXUI_CASE-EVENT_COMPLETION");
    }

    private RequestResponsePact buildPactWithQueryParam(PactDslWithProvider builder, String completionProcessValue) {
        PactDslRequestWithPath pactBuilder = builder
            .given("complete a task using taskId and with completion process")
            .uponReceiving("taskId to complete a task")
            .path(WA_COMPLETE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN);

            pactBuilder = pactBuilder.query(REQ_PARAM_COMPLETION_PROCESS + "=" + completionProcessValue);

        return pactBuilder
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "pactWithCompletionProcessUser", pactVersion = PactSpecVersion.V3)
    void testWithCompletionProcessUser(MockServer mockServer) {
        sendPostWithCompletionProcess(mockServer, "EXUI_USER_COMPLETION");
    }

    @Test
    @PactTestFor(pactMethod = "pactWithCompletionProcessCaseEvent", pactVersion = PactSpecVersion.V3)
    void testWithCompletionProcessCaseEvent(MockServer mockServer) {
        sendPostWithCompletionProcess(mockServer, "EXUI_CASE-EVENT_COMPLETION");
    }

    @Test
    @PactTestFor(pactMethod = "executeCompleteTaskById204", pactVersion = PactSpecVersion.V3)
    void testCompleteTaskByTaskId204Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .post(mockServer.getUrl() + WA_COMPLETE_TASK_BY_ID)
            .then()
            .statusCode(204);
    }

    @Test
    @PactTestFor(pactMethod = "executeCompleteTaskById204WithAssignAndComplete", pactVersion = PactSpecVersion.V3)
    void testCompleteTaskByTaskId204WithAssignAndCompleteTest(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createCompleteTaskRequest())
            .post(mockServer.getUrl() + WA_COMPLETE_TASK_BY_ID)
            .then()
            .statusCode(204);
    }

    private String createCompleteTaskRequest() {

        return "{\n"
               + "  \"completion_options\": {\n"
               + "    \"assign_and_complete\": true\n"
               + "  }\n"
               + "}";

    }

    private void sendPostWithCompletionProcess(MockServer mockServer, String queryParam) {
        String url = mockServer.getUrl() + WA_COMPLETE_TASK_BY_ID;
        url += "?" + REQ_PARAM_COMPLETION_PROCESS + "=" + queryParam;
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .post(url)
            .then()
            .statusCode(204);

    }
}
