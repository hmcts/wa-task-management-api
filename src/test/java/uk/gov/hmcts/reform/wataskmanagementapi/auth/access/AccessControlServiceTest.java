package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private IdamService idamService;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    private AccessControlService accessControlService;

    @BeforeEach
    public void setUp() {
        accessControlService = new AccessControlService(idamService, roleAssignmentService);
    }

    @Test
    void should_succeed_and_return_role_assignments() {

        final Assignment mockedRoleAssignments = mock(Assignment.class);
        final UserInfo mockedUserInfo = mock(UserInfo.class);
        final String idamToken = "someToken";

        when(idamService.getUserInfo(idamToken)).thenReturn(mockedUserInfo);
        when(roleAssignmentService.getRolesForUser(mockedUserInfo.getUid(), idamToken))
            .thenReturn(Collections.singletonList(mockedRoleAssignments));
        AccessControlResponse result = accessControlService.getRoles(idamToken);

        assertNotNull(result);
        assertEquals(mockedUserInfo, result.getUserInfo());
        assertEquals(mockedRoleAssignments, result.getRoleAssignments().get(0));
        verify(idamService, times(1)).getUserInfo(idamToken);
        verifyNoMoreInteractions(idamService);
        verify(roleAssignmentService, times(1)).getRolesForUser(mockedUserInfo.getUid(), idamToken);
        verifyNoMoreInteractions(roleAssignmentService);
    }

    @Test
    void given_user_id_should_return_roles_assignments() {

        when(roleAssignmentService.getRolesForUser("some user id", "Bearer user token"))
            .thenReturn(Collections.singletonList(Assignment.builder().build()));

        AccessControlResponse actualAccessResponse = accessControlService.getRolesGivenUserId(
            "some user id",
            "Bearer user token"
        );

        AccessControlResponse expectedAccessResponse = new AccessControlResponse(
            UserInfo.builder().uid("some user id").build(),
            Collections.singletonList(Assignment.builder().build())
        );
        assertThat(actualAccessResponse)
            .isEqualTo(expectedAccessResponse);

    }
}
