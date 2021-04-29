package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;

import java.io.IOException;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "wa_task_management_api_get_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerGetTaskByIdTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_GET_TASK_BY_ID = WA_URL + "/" + TASK_ID;

    @Pact(provider = "wa_task_management_api_get_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetTaskById200(PactDslWithProvider builder) {

        return builder
            .given("appropriate task is returned")
            .uponReceiving("taskId to get a task by id")
            .path(WA_GET_TASK_BY_ID)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .toPact();
    }

    private PactDslJsonBody createTaskResponse() {
        return new PactDslJsonBody()
            .stringType("id", "7694d1ec-1f0b-4256-82be-a8309ab99136")
            .stringValue("name", "JakeO")
            .stringType("type", "ReviewTheAppeal")
            .stringType("taskState", "unconfigured")
            .stringType("taskSystem", "main")
            .stringType("securityClassification", "PRIVATE")
            .stringType("taskTitle", "review");
    }

    @Test
    @PactTestFor(pactMethod = "executeGetTaskById200")
    void testAssignTaskByTaskId204Test(MockServer mockServer) throws IOException {
        SerenityRest
            .given()
            .headers(getTaskByIdResponseHeaders())
            .contentType(ContentType.JSON)
            .get(mockServer.getUrl() + WA_GET_TASK_BY_ID)
            .then()
            .statusCode(200);
    }

    private Map<String, String> getTaskByIdResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN);
        responseHeaders.put(AUTHORIZATION, AUTH_TOKEN);

        return responseHeaders;
    }
}
