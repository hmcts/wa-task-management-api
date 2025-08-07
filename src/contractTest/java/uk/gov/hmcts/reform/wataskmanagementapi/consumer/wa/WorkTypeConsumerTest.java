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

@PactTestFor(providerName = "wa_task_management_api_get_work_types", port = "8991")
@Import(TaskManagementProviderTestConfiguration.class)
public class WorkTypeConsumerTest extends SpringBootContractBaseTest {

    @MockitoBean
    EntityManagerFactory entityManagerFactory;

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
            .body(createResponseForGetWorkTypesByUserId())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetAllWorkTypes200", pactVersion = PactSpecVersion.V3)
    void testGetAllWorkTypes(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .get(mockServer.getUrl() + WA_URL)
            .then()
            .statusCode(200);
    }

    @Test
    @PactTestFor(pactMethod = "executeGetWorkTypesByUserId200", pactVersion = PactSpecVersion.V3)
    void testGetAllWorkTypesByUserId(MockServer mockServer) {

        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .get(mockServer.getUrl() + WA_URL + "?filter-by-user=true")
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

    private DslPart createResponseForGetWorkTypes() {
        return newJsonBody(
            o -> o
                .array("work_types", workType -> workType
                    .object((value) -> {
                        value
                            .stringType("id", "hearing_work")
                            .stringType("label", "Hearing Work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "upper_tribunal")
                            .stringType("label", "Upper Tribunal");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "routine_work")
                            .stringType("label", "Routine work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "decision_making_work")
                            .stringType("label", "Decision-making work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "applications")
                            .stringType("label", "Applications");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "priority")
                            .stringType("label", "Priority");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "access_requests")
                            .stringType("label", "Access requests");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "error_management")
                            .stringType("label", "Error management");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "review_case")
                            .stringType("label", "Review Case");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "evidence")
                            .stringType("label", "Evidence");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "follow_up")
                            .stringType("label", "Follow Up");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "pre_hearing")
                            .stringType("label", "Pre-Hearing");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "post_hearing")
                            .stringType("label", "Post-Hearing");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "intermediate_track_hearing_work")
                            .stringType("label", "Intermediate track hearing work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "multi_track_hearing_work")
                            .stringType("label", "Multi track hearing work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "intermediate_track_decision_making_work")
                            .stringType("label", "Intermediate track decision making work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "multi_track_decision_making_work")
                            .stringType("label", "Multi track decision making work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "query_work")
                            .stringType("label", "Query work");
                    })
                    .object((value) -> {
                        value
                            .stringType("id", "welsh_translation_work")
                            .stringType("label", "Welsh translation work");
                    })
                )
        ).build();
    }

    private DslPart createResponseForGetWorkTypesByUserId() {
        return newJsonBody(
            o -> o
                .array("work_types", workType -> workType
                    .object((value) -> {
                        value
                            .stringType("id", "hearing_work")
                            .stringType("label", "Hearing Work");
                    })
                )
        ).build();
    }

}
