package uk.gov.hmcts.reform.wataskmanagementapi.consumer.wa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;

import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@PactTestFor(providerName = "wa_task_management_api_get_task_types_by_jurisdiction", port = "8991")
@Import(TaskManagementProviderTestConfiguration.class)
public class TaskTypeConsumerTest extends SpringBootContractBaseTest {

    @MockitoBean
    EntityManagerFactory entityManagerFactory;

    public static final String CONTENT_TYPE = "Content-Type";
    private static final String WA_URL = "/task/task-types";
    private static final String BY_JURISDICTION = "jurisdiction=wa";

    @Pact(provider = "wa_task_management_api_get_task_types_by_jurisdiction", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetAllTaskTypesByJurisdiction200(PactDslWithProvider builder) {
        return builder
            .given("retrieve all task types by jurisdiction")
            .uponReceiving("retrieve all task types by jurisdiction")
            .path(WA_URL)
            .query(BY_JURISDICTION)
            .method(HttpMethod.GET.toString())
            .headers(getTaskManagementServiceResponseHeaders())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createResponseForGetTaskTypes())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetAllTaskTypesByJurisdiction200", pactVersion = PactSpecVersion.V3)
    void testGetAllTaskTypes(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .get(mockServer.getUrl() + WA_URL + "?jurisdiction=wa")
            .then()
            .statusCode(200);
    }

    private Map<String, String> getTaskManagementServiceResponseHeaders() {
        return ImmutableMap.<String, String>builder()
            .put(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .put(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .put(AUTHORIZATION, AUTH_TOKEN)
            .build();
    }

    private DslPart createResponseForGetTaskTypes() {
        return newJsonBody(
            o -> o
                .minArrayLike("task_types", 1, 1, requestedRoles -> requestedRoles
                    .object("task_type", attribute -> attribute
                        .stringType("task_type_id", "someTaskTypeId")
                        .stringType("task_type_name", "Some task type name")
                    )
                )
        ).build();
    }
}
