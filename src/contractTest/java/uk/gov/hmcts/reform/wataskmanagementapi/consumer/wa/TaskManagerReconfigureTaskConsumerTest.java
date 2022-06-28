package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_reconfigure_task_by_case_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
public class TaskManagerReconfigureTaskConsumerTest extends SpringBootContractBaseTest {

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_URL = "/task";
    private static final String WA_RECONFIGURE_TASK_BY_ID = WA_URL + "/operation";

    @Test
    @PactTestFor(pactMethod = "markTaskToReconfigureById204", pactVersion = PactSpecVersion.V3)
    void testMarkTaskToReconfigureById204Test(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getTaskManagementServiceResponseHeaders())
            .contentType(ContentType.JSON)
            .body(createMarkTaskToReconfigureOperationRequest())
            .post(mockServer.getUrl() + WA_RECONFIGURE_TASK_BY_ID)
            .then()
            .statusCode(204);

    }

    @Pact(provider = "wa_task_management_api_reconfigure_task_by_case_id", consumer = "wa_task_management_api")
    public RequestResponsePact markTaskToReconfigureById204(PactDslWithProvider builder) {

        return builder
            .given("reconfigure a task using caseId")
            .uponReceiving("caseId to reconfigure tasks")
            .path(WA_RECONFIGURE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .body(createMarkTaskToReconfigureOperationRequest(), String.valueOf(ContentType.JSON))
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeReconfigurationTaskById204", pactVersion = PactSpecVersion.V3)
    void testExecuteReconfigureTaskByTaskId204Test(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getTaskManagementServiceResponseHeaders())
            .contentType(ContentType.JSON)
            .body(createExecuteReconfigureTaskOperationRequest())
            .post(mockServer.getUrl() + WA_RECONFIGURE_TASK_BY_ID)
            .then()
            .statusCode(204);

    }

    @Pact(provider = "wa_task_management_api_reconfigure_task_by_case_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeReconfigurationTaskById204(PactDslWithProvider builder) {

        return builder
            .given("reconfigure a task")
            .uponReceiving("reconfigure_request_time")
            .path(WA_RECONFIGURE_TASK_BY_ID)
            .method(HttpMethod.POST.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .body(createExecuteReconfigureTaskOperationRequest(), String.valueOf(ContentType.JSON))
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    private Map<String, String> getTaskManagementServiceResponseHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .build();
    }


    private String createMarkTaskToReconfigureOperationRequest() {

        return "{\n"
               + "    \"operation\": \n"
               + "        {\n"
               + "            \"runId\": \"runid1\",\n"
               + "            \"name\": \"MARK_TO_RECONFIGURE\"\n"
               + "        },\n"
               + "    \"taskFilter\": [\n"
               + "        {\n"
               + "            \"@type\": \"MarkTaskToReconfigureTaskFilter\",\n"
               + "            \"key\": \"case_id\",\n"
               + "            \"values\": [\n"
               + "                  \"caseId100\""
               + "              ],\n"
               + "            \"operator\": \"IN\"\n"
               + "        }\n"
               + "    ]\n"
               + "}";
    }

    private String createExecuteReconfigureTaskOperationRequest() {

        return "{\n"
            + "    \"operation\": \n"
            + "        {\n"
            + "            \"runId\": \"runid1\",\n"
            + "            \"name\": \"EXECUTE_RECONFIGURE\"\n"
            + "        },\n"
            + "    \"taskFilter\": [\n"
            + "        {\n"
            + "            \"@type\": \"ExecuteReconfigureTaskFilter\",\n"
            + "            \"key\": \"reconfigure_request_time\",\n"
            + "            \"value\": \"2022-06-06T16:00:00.00000+01:00\",\n"
            + "            \"operator\": \"AFTER\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";
    }
}
