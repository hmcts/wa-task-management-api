package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
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

import java.io.IOException;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "wa_task_management_api_complete_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerCompleteTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_COMPLETE_TASK_BY_ID = WA_URL + "/" + TASK_ID + "/" + "complete";

    @Test
    @PactTestFor(pactMethod = "executeCompleteTaskById204")
    void testCompleteTaskByTaskId204Test(MockServer mockServer) throws IOException {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .post(mockServer.getUrl() + WA_COMPLETE_TASK_BY_ID)
            .then()
            .statusCode(204);
    }

    @Test
    @PactTestFor(pactMethod = "executeCompleteTaskById204WithAssignAndComplete")
    void testCompleteTaskByTaskId204WithAssignAndCompleteTest(MockServer mockServer) throws IOException {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body(createCompleteTaskRequest())
            .post(mockServer.getUrl() + WA_COMPLETE_TASK_BY_ID)
            .then()
            .statusCode(204);
    }

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

    @Pact(
        provider = "wa_task_management_api_complete_task_by_id",
        consumer = "wa_task_management_api"
    )
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

    private String createCompleteTaskRequest() {
        String request = "{\n"
                         + "  \"completion_options\": {\n"
                         + "    \"assign_and_complete\": true\n"
                         + "  }\n"
                         + "}";
        return request;
    }
}
