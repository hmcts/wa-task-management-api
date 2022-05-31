package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
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

@PactTestFor(providerName = "wa_task_management_api_reconfigure_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerReconfigureTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String WA_URL = "/task";
    private static final String WA_RECONFIGURE_TASK_BY_ID = WA_URL + "/operation";

    @Test
    @PactTestFor(pactMethod = "executeReconfigureTaskById204", pactVersion = PactSpecVersion.V3)
    void testReconfigureTaskByTaskId204Test(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body("")
            .post(mockServer.getUrl() + WA_RECONFIGURE_TASK_BY_ID)
            .then()
            .statusCode(204);

    }

    @Pact(provider = "wa_task_management_api_reconfigure_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeReconfigureTaskById204(PactDslWithProvider builder) {

        return builder
            .given("reconfigure a task using caseId")
            .uponReceiving("caseId to reconfigure tasks")
            .path(WA_RECONFIGURE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }
}