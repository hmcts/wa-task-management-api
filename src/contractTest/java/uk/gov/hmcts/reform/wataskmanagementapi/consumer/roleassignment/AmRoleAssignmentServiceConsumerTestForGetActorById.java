package uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;

import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "am_roleAssignment_getAssignment", port = "8991")
@ContextConfiguration(classes = {RoleAssignmentConsumerApplication.class})
public class AmRoleAssignmentServiceConsumerTestForGetActorById extends SpringBootContractBaseTest {

    private static final String ORG_ROLE_ACTOR_ID = "23486";
    private static final String RAS_GET_ACTOR_BY_ID_URL = "/am/role-assignments/actors/";

    @Autowired
    RoleAssignmentServiceApi roleAssignmentApi;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        roleAssignmentService = new RoleAssignmentService(roleAssignmentApi, authTokenGenerator);
    }

    @Test
    @PactTestFor(pactMethod = "executeGetActorByIdOrgRoleAssignmentAndGet200", pactVersion = PactSpecVersion.V3)
    void verifyGetActorById() {
        List<RoleAssignment> roleAssignmentsResponse =
            roleAssignmentService.getRolesForUser(ORG_ROLE_ACTOR_ID, AUTH_TOKEN);

        assertThat(roleAssignmentsResponse.get(0).getActorId(), is(ORG_ROLE_ACTOR_ID));
    }

    @Pact(provider = "am_roleAssignment_getAssignment", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetActorByIdOrgRoleAssignmentAndGet200(PactDslWithProvider builder) {

        return builder
            .given("An actor with provided id is available in role assignment service")
            .uponReceiving(
                "Provider receives a GET /am/role-assignments/actors/{user-id} request from a WA API")
            .path(RAS_GET_ACTOR_BY_ID_URL + ORG_ROLE_ACTOR_ID)
            .method(HttpMethod.GET.toString())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(getResponseHeaders())
            .body(createResponseForOrgRoleAssignment())
            .toPact();
    }

    private DslPart createResponseForOrgRoleAssignment() {
        return newJsonBody(o -> o
            .minArrayLike(
                "roleAssignmentResponse", 1, 1,
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
                    .object("attributes", attribute -> attribute
                        .stringType("primaryLocation", "198444")
                        .stringType("jurisdiction", "IA")
                    )
            )).build();
    }

    private Map<String, String> getResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put(
            "Content-Type",
            "application/vnd.uk.gov.hmcts.role-assignment-service.get-assignments+json;charset=UTF-8;version=1.0"
        );
        return responseHeaders;
    }

}