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

@PactTestFor(providerName = "wa_task_management_api_assign_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerAssignTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_ASSIGN_TASK_BY_ID = WA_URL + "/" + TASK_ID + "/" + "assign";

    @Pact(provider = "wa_task_management_api_assign_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeAssignTaskById204(PactDslWithProvider builder) {

        return builder
            .given("assign a task using taskId")
            .uponReceiving("taskId to assign a task")
            .path(WA_ASSIGN_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeAssignTaskById204")
    void testAssignTaskByTaskId204Test(MockServer mockServer) throws IOException {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body("")
            .post(mockServer.getUrl() + WA_ASSIGN_TASK_BY_ID)
            .then()
            .statusCode(204);
    }
}
