package uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment;

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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "am_role_assignment_service_delete_actor", port = "8991")
@ContextConfiguration(classes = {RoleAssignmentConsumerApplication.class})
public class AmRoleAssignmentServiceConsumerTestForDeleteByAssignmentId extends SpringBootContractBaseTest {

    private static final String ASSIGMENT_ID = "704c8b1c-e89b-436a-90f6-953b1dc40157";
    private static final String AM_RAS_URL = "/am/role-assignments";
    private static final String RAS_DELETE_ACTOR_BY_ID = AM_RAS_URL + "/" + ASSIGMENT_ID;

    @Pact(provider = "am_role_assignment_service_delete_actor", consumer = "wa_task_management_api")
    public RequestResponsePact executeDeleteActorByIdAndGet204(PactDslWithProvider builder) {

        return builder
            .given("An actor with provided id is available in role assignment service")
            .uponReceiving("RAS takes s2s/auth token and returns actor information")
            .path(RAS_DELETE_ACTOR_BY_ID)
            .method(HttpMethod.DELETE.toString())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.NO_CONTENT.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeDeleteActorByIdAndGet204")
    void deleteActorByIdAndGet204Test(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHttpHeaders())
            .contentType(ContentType.JSON)
            .delete(mockServer.getUrl() + RAS_DELETE_ACTOR_BY_ID)
            .then()
            .statusCode(204);
    }

}
