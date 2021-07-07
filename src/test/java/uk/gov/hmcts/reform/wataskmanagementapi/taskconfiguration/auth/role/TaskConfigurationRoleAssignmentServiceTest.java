package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskConfigurationRoleAssignmentServiceTest {

    @Mock
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Mock
    private IdamTokenGenerator idamTokenGenerator;

    private TaskConfigurationRoleAssignmentService roleAssignmentService;

    @BeforeEach
    public void setUp() {
        roleAssignmentService = new TaskConfigurationRoleAssignmentService(
            roleAssignmentServiceApi,
            serviceAuthTokenGenerator,
            idamTokenGenerator
        );
    }

    @Test
    void should_search_roles_by_case_id() {
        final GetRoleAssignmentResponse roleAssignmentResource =
            new GetRoleAssignmentResponse(List.of(getRoleAssignment()));

        final String caseId = UUID.randomUUID().toString();
        final String userToken = "userToken";
        final String s2sToken = "s2sToken";

        when(idamTokenGenerator.generate()).thenReturn(userToken);
        when(serviceAuthTokenGenerator.generate()).thenReturn(s2sToken);

        when(roleAssignmentServiceApi.queryRoleAssignments(
            eq(userToken),
            eq(s2sToken),
            any(QueryRequest.class)
        ))
            .thenReturn(roleAssignmentResource);

        final List<Assignment> actualRoleAssignments = roleAssignmentService.searchRolesByCaseId(caseId);

        assertEquals(1, actualRoleAssignments.size());
    }

    private Assignment getRoleAssignment() {
        final String testUserId = UUID.randomUUID().toString();
        return Assignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId(testUserId)
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();
    }

}
