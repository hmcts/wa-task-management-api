package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class AssignTaskTest extends CamundaHelpers {

    @Mock
    CamundaService camundaService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    IdamTokenGenerator idamTokenGenerator;

    @Mock
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private List<TaskOperationPerformService> taskOperationPerformServices;

    @Mock
    TerminationProcessHelper terminationProcessHelper;

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

    @Test
    void assignTask_should_succeed() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignmentAssigner = mock(RoleAssignment.class);
        when(roleAssignmentAssigner.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignmentAssigners = singletonList(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigners);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignmentAssignee = mock(RoleAssignment.class);
        when(roleAssignmentAssignee.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignmentAssignees = singletonList(roleAssignmentAssignee);
        when(assigneeAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssignees);
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        TaskResource taskResource = spy(TaskResource.class);
        when(cftQueryService.getTask(
            any(), anyList(), any(PermissionRequirements.class))
        ).thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(
            taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
        ).thenReturn(Optional.of(taskResource));

        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

        taskManagementService.assignTask(taskId, assignerAccessControlResponse,
            Optional.of(assigneeAccessControlResponse));
        verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, false);
    }

    @Test
    void assignTask_should_throw_role_assignment_verification_exception_when_assigner_has_access_returns_false() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignmentAssigner = mock(RoleAssignment.class);
        when(roleAssignmentAssigner.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignmentAssigners = singletonList(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigners);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(cftQueryService.getTask(
            any(), anyList(), any(PermissionRequirements.class)
        )).thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
            taskManagementService.assignTask(
                taskId,
                assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            ));
        assertEquals(
            "Role Assignment Verification: "
            + "The user assigning the Task has failed the Role Assignment checks performed.",
            exception.getMessage()
        );

        verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
    }

    @Test
    void assignTask_should_throw_role_assignment_verification_exception_when_assignee_has_access_returns_false() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignmentAssigner = mock(RoleAssignment.class);
        when(roleAssignmentAssigner.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignmentAssigners = singletonList(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigners);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignmentAssignee = singletonList(roleAssignment);
        when(assigneeAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssignee);
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        TaskResource taskResource = spy(TaskResource.class);

        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(
            any(), anyList(), any(PermissionRequirements.class))
        ).thenReturn(Optional.of(taskResource));

        PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(
            taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
        ).thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdOnly(taskId))
            .thenReturn(Optional.of(taskResource));

        Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
            taskManagementService.assignTask(
                taskId,
                assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            ));
        assertEquals(
            "Role Assignment Verification: "
            + "The user being assigned the Task has failed the Role Assignment checks performed.",
            exception.getMessage()
        );

        verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
    }

    @Test
    void assignTask_should_throw_exception_when_missing_required_arguments() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        when(assignerAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);

        Exception exception = assertThrowsExactly(NullPointerException.class, () ->
            taskManagementService.assignTask(
                taskId,
                assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            ));
        assertEquals(
            "Assigner userId cannot be null",
            exception.getMessage()
        );

        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(null).build());

        TaskResource taskResource = spy(TaskResource.class);
        when(cftQueryService.getTask(
            any(), anyList(), any(PermissionRequirements.class))
        ).thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        exception = assertThrowsExactly(NullPointerException.class, () ->
            taskManagementService.assignTask(
                taskId,
                assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
            ));
        assertEquals(
            "Assignee userId cannot be null",
            exception.getMessage()
        );

    }

}

