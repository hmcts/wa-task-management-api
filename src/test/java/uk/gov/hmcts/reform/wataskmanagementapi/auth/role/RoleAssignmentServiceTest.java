package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.TaskConfigurationRoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {
    private static final String IDAM_USER_TOKEN = "IDAM_USER_TOKEN";
    private static final String S2S_TOKEN = "S2S_SERVICE_TOKEN";

    @Mock
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Mock
    private RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Mock
    private IdamTokenGenerator idamTokenGenerator;

    private TaskConfigurationRoleAssignmentService roleAssignmentService;

    @Captor
    private ArgumentCaptor<MultipleQueryRequest> captor;

    private String caseId;
    private RoleAssignment testRoleAssignment;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID().toString();
        testRoleAssignment = getRoleAssignment();
        roleAssignmentService = new TaskConfigurationRoleAssignmentService(
            roleAssignmentServiceApi,
            serviceAuthTokenGenerator,
            idamTokenGenerator
        );

        when(idamTokenGenerator.generate()).thenReturn(IDAM_USER_TOKEN);
        when(serviceAuthTokenGenerator.generate()).thenReturn(S2S_TOKEN);

    }

    @Test
    void should_search_roles_by_case_id() {

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(testRoleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        final List<RoleAssignment> actualRoleAssignments = roleAssignmentService.searchRolesByCaseId(caseId);

        assertNotNull(actualRoleAssignments);
        assertEquals(1, actualRoleAssignments.size());

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        QueryRequest actualQueryRequest = queryRequests.getQueryRequests().get(0);
        assertThat(actualQueryRequest.getRoleType()).contains(RoleType.CASE);
        assertThat(actualQueryRequest.getRoleName()).contains("tribunal-caseworker");
        assertThat(actualQueryRequest.getValidAt()).isBefore(LocalDateTime.now());
        assertThat(actualQueryRequest.getHasAttributes()).contains("caseId");
        assertThat(actualQueryRequest.getAttributes()).isNotNull();
        assertThat(actualQueryRequest.getAttributes().get("caseId")).contains(caseId);
    }

    @Test
    void should_query_roles_for_auto_assignment_by_case_id() {

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(testRoleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        final List<RoleAssignment> actualRoleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(
            createTestTaskWithRoleResources(
                SecurityClassification.PUBLIC,
                singleton(taskRoleResource("tribunal-caseworker", true))
            )
        );

        assertNotNull(actualRoleAssignments);
        assertEquals(1, actualRoleAssignments.size());

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        QueryRequest actualQueryRequest = queryRequests.getQueryRequests().get(0);
        assertThat(actualQueryRequest.getRoleName()).contains("tribunal-caseworker");
        assertThat(actualQueryRequest.getValidAt()).isBefore(LocalDateTime.now());
        assertThat(actualQueryRequest.getHasAttributes()).contains("caseId");
        assertThat(actualQueryRequest.getAttributes()).isNotNull();
    }

    @Test
    void should_filter_non_auto_assignment_roles_when_query_for_auto_assignment() {

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            singletonList(testRoleAssignment)
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        final List<RoleAssignment> actualRoleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(
            createTestTaskWithRoleResources(
                SecurityClassification.RESTRICTED,
                Set.of(taskRoleResource("tribunal-caseworker", true), taskRoleResource("tribunal-caseworker-2", false))
            )
        );

        assertNotNull(actualRoleAssignments);
        assertEquals(1, actualRoleAssignments.size());

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        QueryRequest actualQueryRequest = queryRequests.getQueryRequests().get(0);
        assertThat(actualQueryRequest.getRoleName()).contains("tribunal-caseworker");
        assertThat(actualQueryRequest.getValidAt()).isBefore(LocalDateTime.now());
        assertThat(actualQueryRequest.getHasAttributes()).contains("caseId");
        assertThat(actualQueryRequest.getAttributes()).isNotNull();
    }

    @Test
    void should_query_roles_for_auto_assignment_by_case_id_when_no_results_exists() {

        final RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            Collections.emptyList()
        );

        when(roleAssignmentServiceApi.queryRoleAssignments(eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            any(MultipleQueryRequest.class)))
            .thenReturn(roleAssignmentResource);

        final List<RoleAssignment> actualRoleAssignments = roleAssignmentService.queryRolesForAutoAssignmentByCaseId(
            createTestTaskWithRoleResources(
                SecurityClassification.PRIVATE,
                Set.of(taskRoleResource("tribunal-caseworker", true))
            )
        );

        assertNotNull(actualRoleAssignments);
        assertEquals(0, actualRoleAssignments.size());

        verify(roleAssignmentServiceApi).queryRoleAssignments(
            eq(IDAM_USER_TOKEN),
            eq(S2S_TOKEN),
            captor.capture()
        );

        MultipleQueryRequest queryRequests = captor.getValue();

        assertThat(queryRequests).isNotNull();
        assertThat(queryRequests.getQueryRequests()).isNotEmpty();
        QueryRequest actualQueryRequest = queryRequests.getQueryRequests().get(0);
        assertThat(actualQueryRequest.getRoleName()).contains("tribunal-caseworker");
        assertThat(actualQueryRequest.getValidAt()).isBefore(LocalDateTime.now());
        assertThat(actualQueryRequest.getHasAttributes()).contains("caseId");
        assertThat(actualQueryRequest.getAttributes()).isNotNull();
    }

    @Test
    void should_throw_server_error_exception_when_call_to_role_assignment_fails() {

        doThrow(FeignException.FeignServerException.class)
            .when(roleAssignmentServiceApi).queryRoleAssignments(eq(IDAM_USER_TOKEN),
                eq(S2S_TOKEN),
                any(MultipleQueryRequest.class));

        assertThatThrownBy(() -> roleAssignmentService.searchRolesByCaseId(caseId))
            .isInstanceOf(ServerErrorException.class)
            .hasCauseInstanceOf(FeignException.class)
            .hasMessage("Could not retrieve role assignments when performing the search");

    }

    private RoleAssignment getRoleAssignment() {
        final String testUserId = UUID.randomUUID().toString();
        return RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId(testUserId)
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();
    }

    private TaskResource createTestTaskWithRoleResources(
        SecurityClassification classification,
        Set<TaskRoleResource> taskResourceList) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId",
            taskResourceList
        );
        taskResource.setSecurityClassification(classification);
        return taskResource;
    }

    private TaskRoleResource taskRoleResource(String name, boolean autoAssign) {
        return new TaskRoleResource(
            name,
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            0,
            autoAssign
        );
    }
}
