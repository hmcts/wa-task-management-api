package uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;

import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AmRoleAssignmentServiceConsumerTestForGetActorById extends SpringBootContractBaseTest {

    private static final String ORG_ROLE_ACTOR_ID = "23486";
    private static final String RAS_GET_ACTOR_BY_ID_URL = "/am/role-assignments/actors/";


    @Pact(provider = "am_role_assignment_service_get_actor_by_id", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetActorByIdOrgRoleAssignmentAndGet200(PactDslWithProvider builder) {

        return builder
            .given("An actor with provided id and organisational role assignment "
                   + "is available in role assignment service for a WA API")
            .uponReceiving(
                "Provider receives a GET /am/role-assignments/actors/{user-id} request from a WA API")
            .path(RAS_GET_ACTOR_BY_ID_URL + ORG_ROLE_ACTOR_ID)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(getResponseHeaders())
            .body(createResponseForOrgRoleAssignment())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetActorByIdOrgRoleAssignmentAndGet200")
    void should_get_actor_by_id_and_receive_organisational_role_assignment_with_200_response(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .headers(getHttpHeaders())
                .get(mockServer.getUrl() + RAS_GET_ACTOR_BY_ID_URL + ORG_ROLE_ACTOR_ID)
                .then()
                .extract().asString();

        JSONObject jsonResponse = new JSONObject(actualResponseBody);
        JSONArray roleAssignmentResponse = (JSONArray) jsonResponse.get("roleAssignmentResponse");
        JSONObject first = (JSONObject) roleAssignmentResponse.get(0);
        assertThat(first.get("actorId"), equalTo(ORG_ROLE_ACTOR_ID));
    }

    private DslPart createResponseForOrgRoleAssignment() {
        return newJsonBody(o -> o
            .minArrayLike("roleAssignmentResponse", 1, 1,
                roleAssignmentResponse -> roleAssignmentResponse
                    .stringType("id", "7694d1ec-1f0b-4256-82be-a8309ab99136")
                    .stringValue("actorIdType", "IDAM")
                    .stringValue("actorId", ORG_ROLE_ACTOR_ID)
                    .stringValue("roleType", "ORGANISATION")
                    .stringType("roleName", "tribunal-caseworker")
                    .stringValue("classification", "PUBLIC")
                    .stringValue("grantType", "STANDARD")
                    .stringType("roleCategory", "LEGAL_OPERATIONS")
                    .booleanValue("readOnly", false)
                    .stringType("created", "2021-03-11T14:22:12.961474Z")
                    .object("attributes", attribute -> attribute
                        .stringType("primaryLocation", "198444")
                        .stringType("jurisdiction", "IA")
                    )
            )).build();
    }

    private Map<String, String> getResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type",
            "application/vnd.uk.gov.hmcts.role-assignment-service.get-assignments+json;charset=UTF-8;version=1.0");
        return responseHeaders;
    }

}
