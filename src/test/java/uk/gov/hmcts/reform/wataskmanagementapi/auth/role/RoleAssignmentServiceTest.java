package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleAssignmentServiceApi, authTokenGenerator);
    }

    @Test
    void testGetRolesForUser() {
        final RoleAssignmentResource mockRoleAssignmentResource = mock(RoleAssignmentResource.class);
        String idamUserId = "user";
        String authToken = "token";
        String serviceAuthToken = "authToken";
        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, authToken, serviceAuthToken))
            .thenReturn(mockRoleAssignmentResource);

        List<RoleAssignment> createdRolesForUser = getRoleAssignmentList();
        when(mockRoleAssignmentResource.getRoleAssignmentResponse()).thenReturn(createdRolesForUser);

        List<RoleAssignment> rolesForUser = roleAssignmentService.getRolesForUser(idamUserId, authToken);
        assertEquals(createdRolesForUser, rolesForUser);
        verify(mockRoleAssignmentResource, times(1)).getRoleAssignmentResponse();
    }

    @Test
    void testGetRolesForUserThrowsUnauthorizedException() {
        String idamUserId = "user";
        String authToken = "token";
        String serviceAuthToken = "authToken";
        when(authTokenGenerator.generate()).thenReturn(serviceAuthToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, authToken, serviceAuthToken))
            .thenThrow(FeignException.class);

        assertThrows(UnAuthorizedException.class,
            () -> roleAssignmentService.getRolesForUser(idamUserId, authToken));
    }

    @Test
    void testGetRolesForUserThrowsNullPointerWhenIdamUserIdIsNull() {
        String authToken = "token";

        assertThrows(NullPointerException.class,
            () -> roleAssignmentService.getRolesForUser(null, authToken));
    }

    @NotNull
    private List<RoleAssignment> getRoleAssignmentList() {
        List<RoleAssignment> createdRolesForUser = new ArrayList<>();
        createdRolesForUser.add(getRoleAssignment());
        return createdRolesForUser;
    }

    private RoleAssignment getRoleAssignment() {
        return RoleAssignment.builder().roleName("tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .authorisations(List.of("DIVORCE", "373"))
            .grantType(GrantType.CHALLENGED)
            .attributes(emptyMap())
            .build();
    }
}
