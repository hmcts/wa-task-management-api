package uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import net.serenitybdd.rest.SerenityRest;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;

import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@PactTestFor(providerName = "am_roleAssignment_createAssignment", port = "8991")
@ContextConfiguration(classes = {RoleAssignmentConsumerApplication.class})
public class AmRoleAssignmentServiceConsumerTestForCreateRoleAssignment extends SpringBootContractBaseTest {

    private static final String RAS_CREATE_ROLE_ASSIGNMENT_URL = "/am/role-assignments";

    @Test
    @PactTestFor(pactMethod = "executeCreateRoleAssignmentOneRoleAndGet201")
    void createRoleAssignmentOneRoleAndGet201Test(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .headers(getHttpHeaders())
                .contentType(ContentType.JSON)
                .body(createRoleAssignmentRequest())
                .post(mockServer.getUrl() + RAS_CREATE_ROLE_ASSIGNMENT_URL)
                .then()
                .extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);
        Assertions.assertThat(response).isNotNull();
        JSONObject roleRequest = response.getJSONObject("roleAssignmentResponse").getJSONObject("roleRequest");
        assertThat(roleRequest.get("status"), equalTo("APPROVED"));
        assertThat(roleRequest.get("requestType"), equalTo("CREATE"));
        assertThat(roleRequest.get("replaceExisting"), equalTo(true));

    }

    @Pact(provider = "am_role_assignment_service_create", consumer = "wa_task_management_api")
    public RequestResponsePact executeCreateRoleAssignmentOneRoleAndGet201(PactDslWithProvider builder) {

        return builder
            .given("The assignment request is valid with one requested role and replaceExisting flag as true")
            .uponReceiving("Provider receives a POST /am/role-assignments/actors/{user-id} request from a WA API")
            .path(RAS_CREATE_ROLE_ASSIGNMENT_URL)
            .method(HttpMethod.POST.toString())
            .body(createRoleAssignmentRequest(), String.valueOf(ContentType.JSON))
            .willRespondWith()
            .status(HttpStatus.CREATED.value())
            .headers(getRoleAssignmentResponseHeaders())
            .body(createRoleAssignmentResponse())
            .toPact();
    }

    private String createRoleAssignmentRequest() {
        String request = "";
        request = "{\n"
                  + "  \"requestedRoles\": [\n"
                  + "    {\n"
                  + "      \"actorId\": \"14a21569-eb80-4681-b62c-6ae2ed069e5f\",\n"
                  + "      \"actorIdType\": \"IDAM\",\n"
                  + "      \"classification\": \"PUBLIC\",\n"
                  + "      \"grantType\": \"STANDARD\",\n"
                  + "      \"readOnly\": false,\n"
                  + "      \"roleCategory\": \"LEGAL_OPERATIONS\",\n"
                  + "      \"roleName\": \"tribunal-caseworker\",\n"
                  + "      \"roleType\": \"ORGANISATION\",\n"
                  + "      \"attributes\": {\n"
                  + "        \"jurisdiction\": \"IA\",\n"
                  + "        \"primaryLocation\": \"765324\"\n"
                  + "      }\n"
                  + "    }\n"
                  + "  ],\n"
                  + "  \"roleRequest\": {\n"
                  + "    \"assignerId\": \"14a21569-eb80-4681-b62c-6ae2ed069e4f\",\n"
                  + "    \"process\": \"staff-organisational-role-mapping\",\n"
                  + "    \"reference\": \"14a21569-eb80-4681-b62c-6ae2ed069e5f\",\n"
                  + "    \"replaceExisting\": true\n"
                  + "  }\n"
                  + "}\n";
        return request;
    }

    private DslPart createRoleAssignmentResponse() {
        return newJsonBody(o -> o
            .object("roleAssignmentResponse", ob -> ob
                .object("roleRequest", roleRequest -> roleRequest
                    .stringType("id", "14a21569-eb80-4681-b62c-6ae2ed069e1f")
                    .stringValue("authenticatedUserId",
                        "14a21569-eb80-4681-b62c-6ae2ed069e2f")
                    .stringType("correlationId",
                        "14a21569-eb80-4681-b62c-6ae2ed069e3f")
                    .stringValue("assignerId",
                        "14a21569-eb80-4681-b62c-6ae2ed069e4f")
                    .stringValue("requestType", "CREATE")
                    .stringValue("process", "staff-organisational-role-mapping")
                    .stringValue("reference",
                        "14a21569-eb80-4681-b62c-6ae2ed069e5f")
                    .booleanValue("replaceExisting", true)
                    .stringValue("status", "APPROVED")
                    .stringType("log", "Request has been approved")
                )
                .minArrayLike("requestedRoles", 1, 1, requestedRoles -> requestedRoles
                    .stringType("id", "14a21569-eb80-4681-b62c-6ae2ed069e6f")
                    .stringValue("actorIdType", "IDAM")
                    .stringValue("actorId",
                        "14a21569-eb80-4681-b62c-6ae2ed069e5f")
                    .stringValue("roleType", "ORGANISATION")
                    .stringValue("roleName", "tribunal-caseworker")
                    .stringValue("classification", "PUBLIC")
                    .stringValue("grantType", "STANDARD")
                    .stringValue("roleCategory", "LEGAL_OPERATIONS")
                    .stringValue("process", "staff-organisational-role-mapping")
                    .stringValue("reference",
                        "14a21569-eb80-4681-b62c-6ae2ed069e5f")
                    .stringValue("status", "LIVE")
                    .stringType("log",
                        "Create requested with replace: true\n"
                        + "Create approved : staff_organisational_role_mapping_service_create\n"
                        + "Approved : validate_role_assignment_against_patterns")
                    .object("attributes", attribute -> attribute
                        .stringType("jurisdiction", "IA")
                        .stringType("primaryLocation", "765324")
                    )
                )
            )).build();
    }

    private Map<String, String> getRoleAssignmentResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", "application/vnd.uk.gov.hmcts.role-assignment-service."
                                            + "create-assignments+json");
        return responseHeaders;
    }

}
