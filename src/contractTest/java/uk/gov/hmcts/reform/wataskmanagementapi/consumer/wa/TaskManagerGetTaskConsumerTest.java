package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.CamundaConsumerApplication;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import java.io.IOException;
import java.util.Map;
import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_claim_task_by_id", port = "8991")
@ContextConfiguration(classes = {CamundaConsumerApplication.class})
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskManagerGetTaskConsumerTest extends SpringBootContractBaseTest {
    private static final String TASK_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String WA_URL = "/task";
    private static final String WA_GET_TASK_BY_ID = WA_URL + "/" + TASK_ID;
    public static final String CONTENT_TYPE = "Content-Type";
    @Pact(provider = "wa_task_management_api_get_task_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetTaskById200(PactDslWithProvider builder) {
        return builder
            .given("get a task using taskId")
            .uponReceiving("taskId to get a task")
            .path(WA_GET_TASK_BY_ID)
            .method(HttpMethod.GET.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTask())
            .toPact();
    }
    @Test
    @PactTestFor(pactMethod = "executeGetTaskById200")
    void testGetTaskByTaskId200Test(MockServer mockServer) throws IOException {
        Request.Get(mockServer.getUrl() + WA_GET_TASK_BY_ID)
            .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .addHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .addHeader(AUTHORIZATION, AUTH_TOKEN)
            .execute()
            .returnResponse();
    }

    private DslPart createResponseForGetTask() {
        return newJsonBody(
            o -> o
                .object("task",
                        task -> task
                            .stringType("id", "7694d1ec-1f0b-4256-82be-a8309ab99136")
                            .stringType("name", "JakeO")
                            .stringType("type", "ReviewTheAppeal")
                            .stringType("task_state", "unconfigured")
                            .stringType("task_system", "main")
                            .stringType("security_classification", "PRIVATE")
                            .stringType("task_title", "review")
                            .stringType("assignee", "Mark Alistair")
                            .booleanValue("auto_assigned", true)
                            .stringType("execution_type", "Time extension")
                            .stringType("jurisdiction", "IA")
                            .stringType("region", "South")
                            .stringType("location", "12345")
                            .stringType("location_name", "Newcastle")
                            .stringType("case_type_id", "Asylum")
                            .stringType("case_id", "4d4b3a4e-c91f-433f-92ac-e456ae34f72a")
                            .stringType("case_category", "processApplication")
                            .stringType("case_name", "caseName")
                            .booleanValue("warnings", true)
                )).build();
    }
    private Map<String, String> getTaskManagementServiceResponseHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .put(AUTHORIZATION, AUTH_TOKEN)
            .build();
    }
}
