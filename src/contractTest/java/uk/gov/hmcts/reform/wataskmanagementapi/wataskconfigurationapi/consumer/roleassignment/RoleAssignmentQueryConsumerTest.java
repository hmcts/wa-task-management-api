package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.consumer.roleassignment;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.QueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.consumer.roleassignment.RoleAssignmentConsumerApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService.TOTAL_RECORDS;
import static uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi.V2_MEDIA_TYPE_POST_ASSIGNMENTS;

@SuppressWarnings("checkstyle:LineLength")
@PactTestFor(providerName = "am_roleAssignment_queryAssignment", port = "8991")
@ContextConfiguration(classes = {RoleAssignmentConsumerApplication.class})
public class RoleAssignmentQueryConsumerTest extends SpringBootContractBaseTest {
    private final String assigneeId = "14a21569-eb80-4681-b62c-6ae2ed069e5f";
    private final LocalDateTime validAtDate = LocalDateTime.parse("2021-12-04T00:00:00");

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    RoleAssignmentServiceApi roleAssignmentApi;
    @MockitoBean
    AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private IdamTokenGenerator idamTokenGenerator;
    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH_TOKEN);
        when(idamTokenGenerator.generate()).thenReturn(AUTH_TOKEN);

        roleAssignmentService = new RoleAssignmentService(roleAssignmentApi, authTokenGenerator, idamTokenGenerator, 50);
    }

    @Pact(provider = "am_roleAssignment_queryAssignment", consumer = "wa_task_management_api")
    public RequestResponsePact generatePactFragmentForQueryRoleAssignments(PactDslWithProvider builder) throws JsonProcessingException {
        return builder
            .given("A list of role assignments for the advanced search query")
            .uponReceiving("A query request for roles by caseId")
            .path("/am/role-assignments/query")
            .method(HttpMethod.POST.toString())
            .matchHeader(AUTHORIZATION, AUTH_TOKEN)
            .matchHeader(SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN)
            .matchHeader(
                CONTENT_TYPE,
                "application\\/vnd\\.uk\\.gov\\.hmcts\\.role-assignment-service\\.post-assignment-query-request\\+json\\;charset\\=UTF-8\\;version\\=2\\.0",
                "application/vnd.uk.gov.hmcts.role-assignment-service.post-assignment-query-request+json;charset=UTF-8;version=2.0"
            )
            .body(createRoleAssignmentRequestSearchQueryMultipleRoleAssignments(), V2_MEDIA_TYPE_POST_ASSIGNMENTS)
            .willRespondWith()
            .matchHeader(
                CONTENT_TYPE,
                "application\\/vnd\\.uk\\.gov\\.hmcts\\.role-assignment-service\\.post-assignment-query-request\\+json\\;charset\\=UTF-8\\;version\\=2\\.0",
                "application/vnd.uk.gov.hmcts.role-assignment-service.post-assignment-query-request+json;charset=UTF-8;version=2.0"
            )
            .matchHeader(
                TOTAL_RECORDS,
                "\\d+",
                "1"
            )
            .status(HttpStatus.OK.value())
            .headers(getResponseHeaders())
            .body(createRoleAssignmentResponseSearchQueryResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragmentForQueryRoleAssignments", pactVersion = PactSpecVersion.V3)
    public void verifyQueryRoleAssignments() {
        List<RoleAssignment> queryRoleAssignmentResponse = roleAssignmentService
            .performSearch(buildQueryRequest()).getRoleAssignmentResponse();

        assertThat(queryRoleAssignmentResponse.get(0).getActorId(), is(assigneeId));

    }

    private MultipleQueryRequest buildQueryRequest() {
        String caseId = "1212121212121213";
        QueryRequest queryRequest = QueryRequest.builder()
            .roleType(singletonList(RoleType.CASE))
            .roleName(singletonList("tribunal-caseworker"))
            .validAt(validAtDate)
            .attributes(Map.of("caseId", List.of(caseId)))
            .build();

        return MultipleQueryRequest.builder().queryRequests(singletonList(queryRequest)).build();
    }

    private DslPart createRoleAssignmentResponseSearchQueryResponse() {
        final String id = "9785c98c-78f2-418b-ab74-a892c3ccca9f";

        return newJsonBody(o ->
            o.minArrayLike(
                "roleAssignmentResponse", 1, 1,
                roleAssignmentResponse -> roleAssignmentResponse
                    .stringType("id", id)
                    .stringValue("actorIdType", "IDAM")
                    .stringType("actorId", assigneeId)
                    .stringType("roleName", "senior-tribunal-caseworker")
                    .stringValue("classification", "PRIVATE")
                    .stringValue("grantType", "STANDARD")
                    .stringValue("roleCategory", "LEGAL_OPERATIONS")
                    .numberMatching("created", "^[0-9]+(\\.[0-9]+)?$", new BigDecimal("1758105100.293081000"))
                    .numberMatching("beginTime", "^[0-9]+(\\.[0-9]+)?$", new BigDecimal("1758105100.293081000"))
                    .numberMatching("endTime", "^[0-9]+(\\.[0-9]+)?$", new BigDecimal("1758105100.293081000"))
                    .booleanValue("readOnly", false)
                    .object("attributes", attribute -> attribute
                        .stringType("jurisdiction", "IA")
                        .stringType("primaryLocation", "500A2S"))
                    .minArrayLike("authorisations", 0,
                        PactDslJsonRootValue.stringMatcher("[a-zA-Z]*", "IAC"), 1)
            )).build();
    }

    private String createRoleAssignmentRequestSearchQueryMultipleRoleAssignments() throws JsonProcessingException {
        MultipleQueryRequest queryRequest = buildQueryRequest();
        return objectMapper.writeValueAsString(queryRequest);
    }

    private Map<String, String> getResponseHeaders() {
        Map<String, String> responseHeaders = Maps.newHashMap();
        responseHeaders.put("Content-Type", V2_MEDIA_TYPE_POST_ASSIGNMENTS);
        responseHeaders.put(TOTAL_RECORDS, "1");
        return responseHeaders;
    }

}
