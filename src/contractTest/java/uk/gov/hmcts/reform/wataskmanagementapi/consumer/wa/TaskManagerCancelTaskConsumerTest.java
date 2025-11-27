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
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess.EXUI_USER_CANCELLATION;

@PactTestFor(providerName = "wa_task_management_api_cancel_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerCancelTaskConsumerTest extends SpringBootContractBaseTest {

    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_CANCEL_TASK_BY_ID = WA_URL + "/" + TASK_ID + "/" + "cancel";

    public static final String REQ_PARAM_CANCELLATION_PROCESS = "cancellation_process";


    @Pact(provider = "wa_task_management_api_cancel_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeCancelTaskById204(PactDslWithProvider builder) {

        return builder
            .given("cancel a task using taskId")
            .uponReceiving("taskId to cancel a task")
            .path(WA_CANCEL_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_cancel_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeCancelTaskForGivenIdWithCancellationProcessUser(PactDslWithProvider builder) {
        PactDslRequestWithPath pactBuilder = builder
            .given("cancel a task using taskId and with cancellation process")
            .uponReceiving("taskId to cancel a task")
            .path(WA_CANCEL_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .body("", String.valueOf(ContentType.JSON))
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN);

        pactBuilder = pactBuilder.query(REQ_PARAM_CANCELLATION_PROCESS + "=" + EXUI_USER_CANCELLATION.getValue());

        return pactBuilder
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeCancelTaskById204", pactVersion = PactSpecVersion.V3)
    void testCancelTaskByTaskId204Test(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .body("")
            .post(mockServer.getUrl() + WA_CANCEL_TASK_BY_ID)
            .then()
            .statusCode(204);

    }

    @Test
    @PactTestFor(pactMethod = "executeCancelTaskForGivenIdWithCancellationProcessUser",
        pactVersion = PactSpecVersion.V3)
    void testWithCancellationProcessUser(MockServer mockServer) {
        String url = mockServer.getUrl() + WA_CANCEL_TASK_BY_ID;
        url += "?" + REQ_PARAM_CANCELLATION_PROCESS + "=" + EXUI_USER_CANCELLATION.getValue();
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .post(url)
            .then()
            .statusCode(204);

    }
}
