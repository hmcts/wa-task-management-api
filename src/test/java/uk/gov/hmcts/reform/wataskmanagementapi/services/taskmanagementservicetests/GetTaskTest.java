package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TerminationProcessHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@ExtendWith(MockitoExtension.class)
class GetTaskTest extends CamundaHelpers {

    @Mock
    CamundaService camundaService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    private List<TaskOperationPerformService> taskOperationPerformServices;
    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;
    @Mock
    IdamTokenGenerator idamTokenGenerator;
    @Mock
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @Mock
    TerminationProcessHelper terminationProcessHelper;

    @Test
    void getTask_should_succeed_and_return_mapped_task() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(READ);
        taskId = UUID.randomUUID().toString();
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        Task mockedMappedTask = mock(Task.class);
        when(cftTaskMapper.mapToTaskWithPermissions(any(), any()))
            .thenReturn(mockedMappedTask);

        Task response = taskManagementService.getTask(taskId, accessControlResponse);
        assertNotNull(response);
        assertEquals(mockedMappedTask, response);
    }

    @Test
    void getTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        assertThatThrownBy(() -> taskManagementService.getTask(taskId, accessControlResponse))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
    }

    @BeforeEach
    void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            entityManager,
            idamTokenGenerator,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator,
            terminationProcessHelper);


        taskId = UUID.randomUUID().toString();
    }
}

