package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.consumer.roleassignment;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.TaskConfigurationRoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@PactTestFor(providerName = "am_roleAssignment_queryAssignment", port = "8991")
@ContextConfiguration(classes = {RoleAssignmentConsumerApplication.class})
public class RoleAssignmentQueryConsumerTest extends SpringBootContractBaseTest {


    private final String assigneeId = "14a21569-eb80-4681-b62c-6ae2ed069e5f";
    private final String caseId = "1212121212121213";
    private final LocalDateTime validAtDate = LocalDateTime.parse("2021-12-04T00:00:00");

    @Autowired
    RoleAssignmentServiceApi roleAssignmentApi;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @MockBean
    private IdamTokenGenerator idamTokenGenerator;

    private TaskConfigurationRoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(idamTokenGenerator.generate()).thenReturn(AUTH_TOKEN);

        roleAssignmentService = new TaskConfigurationRoleAssignmentService(
            roleAssignmentApi,
            authTokenGenerator,
            idamTokenGenerator
        );
    }

    @Pact(provider = "am_roleAssignment_queryAssignment", consumer = "wa_task_configuration_api")
    public RequestResponsePact generatePactFragmentForQueryRoleAssignments(PactDslWithProvider builder) {
        return builder
            .given("A list of role assignments for the search query")
            .uponReceiving("A query request for roles by caseId")
            .path("/am/role-assignments/query")
            .method(HttpMethod.POST.toString())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .body(createRoleAssignmentRequestSearchQueryMultipleRoleAssignments())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(getResponseHeaders())
            .body(createRoleAssignmentResponseSearchQueryResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragmentForQueryRoleAssignments")
    public void verifyQueryRoleAssignments() {
        List<RoleAssignment> queryRoleAssignmentResponse = roleAssignmentService
            .performSearch(buildQueryRequest()).getRoleAssignmentResponse();

        assertThat(queryRoleAssignmentResponse.get(0).getActorId(), is(assigneeId));

    }

    private QueryRequest buildQueryRequest() {

        return QueryRequest.builder()
            .roleType(singletonList(RoleType.CASE))
            .roleName(singletonList("tribunal-caseworker"))
            .validAt(validAtDate)
            .attributes(Map.of("caseId", List.of(caseId)))
            .build();
    }

    private DslPart createRoleAssignmentResponseSearchQueryResponse() {
        return newJsonBody(o -> o
            .minArrayLike("roleAssignmentResponse", 1, 1,
                roleAssignmentResponse -> roleAssignmentResponse
                    .stringType("actorId", assigneeId)
            )).build();
    }

    private String createRoleAssignmentRequestSearchQueryMultipleRoleAssignments() {
        return "{\n"
               + "\"roleType\": [\"CASE\"],\n"
               + "\"roleName\": [\"tribunal-caseworker\"],\n"
               + "\"validAt\": \"2021-12-04T00:00:00\",\n"
               + "\"attributes\": {\n"
               + "\"caseId\": [\"" + caseId + "\"]\n"
               + "}\n"
               + "}";
    }

    private Map<String, String> getResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type",
            "application/vnd.uk.gov.hmcts.role-assignment-service.post-assignment-query-request+json;"
            + "charset=UTF-8;version=1.0");
        return responseHeaders;
    }

}
