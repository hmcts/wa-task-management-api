package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    public void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleAssignmentServiceApi, authTokenGenerator, systemUserIdamToken);
    }

    @Test
    void getRolesForUser_should_succeed_and_return_role_assignments() {
        String idamUserId = "someIdamUserId";
        String mockedAuthToken = "authToken";
        String mockedServiceToken = "serviceToken";

        List<Assignment> mockedRoleAssignments = createMockRoleAssignments(idamUserId);
        when(authTokenGenerator.generate()).thenReturn(mockedServiceToken);
        when(roleAssignmentServiceApi.getRolesForUser(idamUserId, mockedAuthToken, mockedServiceToken))
            .thenReturn(new GetRoleAssignmentResponse(mockedRoleAssignments, links));

        List<Assignment> result = roleAssignmentService.getRolesForUser(idamUserId, mockedAuthToken);

        result.forEach(roleAssignment -> {

            assertThat(
                roleAssignment.getRoleName(),
                either(is("someCaseRoleName")).or(is("someOrganisationalRoleName"))
            );
            assertThat(roleAssignment.getRoleType(), either(is(RoleType.ORGANISATION)).or(is(RoleType.CASE)));
            assertEquals(idamUserId, roleAssignment.getActorId());
        });
    }

    @Test
    void getRolesForUser_should_throw_an_InsufficientPermissionsException_when_feign_exception_is_thrown() {

        String idamUserId = "someIdamUserId";
        String mockedAuthToken = "authToken";
        String mockedServiceToken = "serviceToken";

        when(authTokenGenerator.generate()).thenReturn(mockedServiceToken);

        TestFeignClientException exception =
            new TestFeignClientException(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()
            );

        doThrow(exception)
            .when(roleAssignmentServiceApi).getRolesForUser(idamUserId, mockedAuthToken, mockedServiceToken);

        assertThatThrownBy(() -> roleAssignmentService.getRolesForUser(idamUserId, mockedAuthToken))
            .isInstanceOf(UnAuthorizedException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage("User did not have sufficient permissions to perform this action");

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
