package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignments;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    public void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleAssignmentServiceApi, authTokenGenerator);
    }

    @Test
    void getRolesForUser_should_succeed_and_classify_roles_based_on_role_typ() {
        String idamUserId = "someIdamUserId";
        String mockedAuthToken = "authToken";
        String mockedServiceToken = "serviceToken";
        HttpHeaders headers = mock(HttpHeaders.class);

        List<Assignment> mockedRoleAssignments = createMockRoleAssignments(idamUserId);
        when(headers.getFirst(AUTHORIZATION)).thenReturn(mockedAuthToken);
        when(authTokenGenerator.generate()).thenReturn(mockedServiceToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, mockedAuthToken, mockedServiceToken))
            .thenReturn(new RoleAssignmentResponse(mockedRoleAssignments));

        RoleAssignments result = roleAssignmentService.getRolesForUser(idamUserId, headers);

        result.getCaseRoles().forEach(caseRole -> {
            assertEquals(RoleType.CASE, caseRole.getRoleType());
            assertEquals(idamUserId, caseRole.getActorId());
        });

        result.getOrganisationalRoles().forEach(orgRole -> {
            assertEquals(RoleType.ORGANISATION, orgRole.getRoleType());
            assertEquals(idamUserId, orgRole.getActorId());
        });

        assertThat(result.getRoles())
            .hasSize(2)
            .containsExactlyInAnyOrder("someCaseRoleName", "someOrganisationalRoleName");

    }

    private List<Assignment> createMockRoleAssignments(String idamUserId) {
        List<Assignment> mockedAssignments = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Assignment assignment = mock(Assignment.class);
            when(assignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            when(assignment.getRoleName()).thenReturn("someOrganisationalRoleName");
            when(assignment.getActorId()).thenReturn(idamUserId);
            mockedAssignments.add(assignment);
        }
        for (int i = 0; i < 10; i++) {
            Assignment assignment = mock(Assignment.class);
            when(assignment.getRoleType()).thenReturn(RoleType.CASE);
            when(assignment.getRoleName()).thenReturn("someCaseRoleName");
            when(assignment.getActorId()).thenReturn(idamUserId);
            mockedAssignments.add(assignment);
        }
        return mockedAssignments;
    }

}
