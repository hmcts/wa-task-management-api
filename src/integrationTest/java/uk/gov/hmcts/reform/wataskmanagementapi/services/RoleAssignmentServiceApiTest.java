package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationIdamStubConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationSecurityTestConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils.MAX_ROLE_ASSIGNMENT_RECORDS;

@SpringBootTest
@Import({IntegrationSecurityTestConfig.class, IntegrationIdamStubConfig.class})
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoleAssignmentServiceApiTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @Test
    void queryRoleAssignmentTest() throws IOException {

        String roleAssignmentsResponseAsJsonString = loadJsonFileResource();

        stubRoleAssignmentApiResponse(roleAssignmentsResponseAsJsonString);

        RoleAssignmentResource roleAssignmentResource = roleAssignmentServiceApi.queryRoleAssignments(
            "user token",
            "s2s token",
            0,
            MAX_ROLE_ASSIGNMENT_RECORDS,
            MultipleQueryRequest.builder().build()
        ).getBody();

        RoleAssignment expectedRoleAssignment = RoleAssignment.builder()
            .id("428971b1-3954-4783-840f-c2718732b466")
            .actorIdType(ActorIdType.IDAM)
            .actorId("122f8de4-2eb6-4dcf-91c9-16c2c8aaa422")
            .roleType(RoleType.CASE)
            .roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.SPECIFIC)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .readOnly(false)
            .created(OffsetDateTime.parse("2020-11-09T14:32:23.693195Z"))
            .attributes(Map.of(
                RoleAttributeDefinition.CASE_ID.value(), "1604929600826893",
                RoleAttributeDefinition.JURISDICTION.value(), "IA",
                RoleAttributeDefinition.CASE_TYPE.value(), "Asylum"
            ))
            .authorisations(emptyList())
            .build();

        assertThat(roleAssignmentResource.getRoleAssignmentResponse()).isNotEmpty();
        assertThat(roleAssignmentResource.getRoleAssignmentResponse().get(0)).isEqualTo(expectedRoleAssignment);
    }

    @Test
    void queryRoleAssignmentAndReceiveNewRolesTest() throws IOException {

        String roleAssignmentsResponseAsJsonString = loadJsonFileResourceForRoleNameTests();

        stubRoleAssignmentApiResponse(roleAssignmentsResponseAsJsonString);

        RoleAssignmentResource roleAssignmentResource = roleAssignmentServiceApi.queryRoleAssignments(
            "user token",
            "s2s token",
            0,
            MAX_ROLE_ASSIGNMENT_RECORDS,
            MultipleQueryRequest.builder().build()
        ).getBody();

        List<RoleAssignment> expectedRoleAssignments = asList(RoleAssignment.builder()
                .id("428971b1-3954-4783-840f-c2718732b466")
                .actorIdType(ActorIdType.IDAM)
                .actorId("122f8de4-2eb6-4dcf-91c9-16c2c8aaa422")
                .roleType(RoleType.CASE)
                .roleName("case-manager")
                .classification(Classification.RESTRICTED)
                .grantType(GrantType.SPECIFIC)
                .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                .readOnly(false)
                .created(OffsetDateTime.parse("2020-11-09T14:32:23.693195Z"))
                .attributes(Map.of(
                    RoleAttributeDefinition.CASE_ID.value(), "1604929600826893",
                    RoleAttributeDefinition.JURISDICTION.value(), "IA",
                    RoleAttributeDefinition.CASE_TYPE.value(), "Asylum"
                ))
                .authorisations(emptyList())
                .build(),
            RoleAssignment.builder()
                .id("59eeafe3-6512-48fd-84a2-25b1df1d3c40")
                .actorIdType(ActorIdType.IDAM)
                .actorId("93809631-0674-478d-b14a-5bfd2cb19725")
                .roleType(RoleType.CASE)
                .roleName("case-allocator")
                .classification(Classification.RESTRICTED)
                .grantType(GrantType.SPECIFIC)
                .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                .readOnly(false)
                .created(OffsetDateTime.parse("2020-11-09T14:32:23.693195Z"))
                .attributes(Map.of(
                    RoleAttributeDefinition.CASE_ID.value(), "1604929600826893",
                    RoleAttributeDefinition.JURISDICTION.value(), "IA",
                    RoleAttributeDefinition.CASE_TYPE.value(), "Asylum"
                ))
                .authorisations(emptyList())
                .build()
        );

        assertThat(roleAssignmentResource.getRoleAssignmentResponse()).isNotEmpty();
        assertThat(roleAssignmentResource.getRoleAssignmentResponse()).isEqualTo(expectedRoleAssignments);
    }

    @Test
    void queryRoleAssignmentTestWhenValuesAreUnknown() throws IOException {

        String roleAssignmentsResponseAsJsonString = loadJsonFileResourceWithUnknownValues();

        stubRoleAssignmentApiResponse(roleAssignmentsResponseAsJsonString);
        RoleAssignmentResource roleAssignmentResource = roleAssignmentServiceApi.queryRoleAssignments(
            "user token",
            "s2s token",
            0,
            MAX_ROLE_ASSIGNMENT_RECORDS,
            MultipleQueryRequest.builder().build()
        ).getBody();

        RoleAssignment expectedRoleAssignment = RoleAssignment.builder()
            .id("428971b1-3954-4783-840f-c2718732b466")
            .actorIdType(ActorIdType.UNKNOWN)
            .actorId("122f8de4-2eb6-4dcf-91c9-16c2c8aaa422")
            .roleType(RoleType.UNKNOWN)
            .roleName("tribunal-caseworker")
            .classification(Classification.UNKNOWN)
            .grantType(GrantType.UNKNOWN)
            .roleCategory(RoleCategory.UNKNOWN)
            .readOnly(false)
            .created(OffsetDateTime.parse("2020-11-09T14:32:23.693195Z"))
            .attributes(Map.of(
                RoleAttributeDefinition.CASE_ID.value(), "1604929600826893",
                RoleAttributeDefinition.JURISDICTION.value(), "IA",
                RoleAttributeDefinition.CASE_TYPE.value(), "Asylum"
            ))
            .authorisations(emptyList())
            .build();

        assertThat(roleAssignmentResource.getRoleAssignmentResponse()).isNotEmpty();
        assertThat(roleAssignmentResource.getRoleAssignmentResponse().get(0)).isEqualTo(expectedRoleAssignment);
    }

    private void stubRoleAssignmentApiResponse(String roleAssignmentsResponseAsJsonString) {
        wireMockServer.stubFor(post(urlEqualTo("/am/role-assignments/query")).willReturn(
            aResponse()
                .withStatus(200)
                .withHeader(
                    "Content-Type",
                    "application/vnd.uk.gov.hmcts.role-assignment-service"
                    + ".post-assignment-query-request+json;charset=UTF-8;version=2.0"
                )
                .withBody(roleAssignmentsResponseAsJsonString))
        );
    }

    private String loadJsonFileResource() throws IOException {
        return loadFile("roleAssignmentsResponse.json");
    }

    private String loadJsonFileResourceWithUnknownValues() throws IOException {
        return loadFile("roleAssignmentsResponseUnknownValues.json");
    }

    private String loadJsonFileResourceForRoleNameTests() throws IOException {
        return loadFile("roleAssignmentsResponseNewRoles.json");

    }

    private String loadFile(String fileName) throws IOException {
        return FileUtils.readFileToString(ResourceUtils.getFile(
            "classpath:variableextractors/"
            + fileName), StandardCharsets.UTF_8);
    }

}
