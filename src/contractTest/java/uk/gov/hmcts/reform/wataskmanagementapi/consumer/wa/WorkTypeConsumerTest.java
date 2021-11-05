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
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.io.IOException;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_get_work_types", port = "8991")
@Import(TaskManagementProviderTestConfiguration.class)
public class WorkTypeConsumerTest extends SpringBootContractBaseTest {

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_URL = "/work-types";
    private static final String WA_GET_WORK_TYPES_BY_USER_ID = "filter-by-user=true";

    @Pact(provider = "wa_task_management_api_get_work_types", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetAllWorkTypes200(PactDslWithProvider builder) {
        return builder
            .given("retrieve all work types")
            .uponReceiving("retrieve all work types")
            .path(WA_URL)
            .method(HttpMethod.GET.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetWorkTypes())
            .toPact();
    }

    @Pact(provider = "wa_task_management_api_get_work_types", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetWorkTypesByUserId200(PactDslWithProvider builder) {
        return builder
            .given("retrieve work types by userId")
            .uponReceiving("userId to get all work types")
            .path(WA_URL)
            .query(WA_GET_WORK_TYPES_BY_USER_ID)
            .method(HttpMethod.GET.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetWorkTypes())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetAllWorkTypes200")
    void testGetAllWorkTypes(MockServer mockServer) throws IOException {
        Request.Get(mockServer.getUrl() + WA_URL)
            .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .addHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .addHeader(AUTHORIZATION, AUTH_TOKEN)
            .execute()
            .returnResponse();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetWorkTypesByUserId200")
    void testGetAllWorkTypesByUserId(MockServer mockServer) throws IOException {
        Request.Get(mockServer.getUrl() + WA_URL + "?filter-by-user=true")
            .addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .addHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .addHeader(AUTHORIZATION, AUTH_TOKEN)
            .execute()
            .returnResponse();
    }

    private Map<String, String> getTaskManagementServiceResponseHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .put(AUTHORIZATION, AUTH_TOKEN)
            .build();
    }

    private DslPart createResponseForGetWorkTypes() {
        return newJsonBody(
            o -> o
                .minArrayLike("work_types", 1, 1,
                    workType -> workType
                        .stringType("id", "hearing_work")
                        .stringType("label", "Hearing Work")
                )
        ).build();
    }
}
