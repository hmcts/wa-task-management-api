package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

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
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class AssignTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    @Mock
    CamundaService camundaService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    IdamTokenGenerator idamTokenGenerator;
    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private List<TaskOperationService> taskOperationServices;

    @BeforeEach
    public void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService
        );
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            taskOperationServices,
            entityManager,
            idamTokenGenerator
        );


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

        PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        when(cftQueryService.getTask(
            taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
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

        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        when(cftQueryService.getTask(
            taskId,
            assignerAccessControlResponse.getRoleAssignments(),
            requirements
        )).thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        assertThatThrownBy(() -> taskManagementService.assignTask(
                taskId,
                assignerAccessControlResponse,
                Optional.of(assigneeAccessControlResponse)
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: "
                        + "The user assigning the Task has failed the Role Assignment checks performed.");

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
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleType(MANAGE);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(
            taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
        ).thenReturn(Optional.of(taskResource));

        PermissionRequirements otherRequirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(
            taskId, assigneeAccessControlResponse.getRoleAssignments(), otherRequirements)
        ).thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdOnly(taskId))
            .thenReturn(Optional.of(taskResource));

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            Optional.of(assigneeAccessControlResponse)
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: "
                        + "The user being assigned the Task has failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).assignTask(any(), any(), anyBoolean());
    }

    @Test
    void assignTask_should_throw_exception_when_missing_required_arguments() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        when(assignerAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            Optional.of(assigneeAccessControlResponse)
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("Assigner userId cannot be null");

        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(null).build());

        TaskResource taskResource = spy(TaskResource.class);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        when(cftQueryService.getTask(
            taskId, assignerAccessControlResponse.getRoleAssignments(), requirements)
        ).thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            Optional.of(assigneeAccessControlResponse)
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("Assignee userId cannot be null");

    }


}

