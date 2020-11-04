package uk.gov.hmcts.reform.wataskmanagementapi.auth.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignments;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

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
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        final UserInfo mockedUserInfo = mock(UserInfo.class);
        final String idamToken = "someToken";

        when(httpHeaders.getFirst(AUTHORIZATION)).thenReturn(idamToken);
        when(idamService.getUserInfo(idamToken)).thenReturn(mockedUserInfo);
        when(roleAssignmentService.getRolesForUser(mockedUserInfo.getUid(), httpHeaders))
            .thenReturn(Collections.singletonList(mockedRoleAssignments));
        List<Assignment> result = accessControlService.getRoles(httpHeaders);

        assertEquals(mockedRoleAssignments, result.get(0));
        verify(idamService, times(1)).getUserInfo(idamToken);
        verifyNoMoreInteractions(idamService);
        verify(roleAssignmentService, times(1)).getRolesForUser(mockedUserInfo.getUid(), httpHeaders);
        verifyNoMoreInteractions(roleAssignmentService);
    }
}
