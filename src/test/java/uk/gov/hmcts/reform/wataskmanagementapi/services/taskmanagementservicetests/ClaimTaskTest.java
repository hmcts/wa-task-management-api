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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class ClaimTaskTest extends CamundaHelpers {

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


    @Test
    void claimTask_should_succeed() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
        taskManagementService.claimTask(taskId, accessControlResponse);

        verify(camundaService, times(1))
            .assignTask(taskId, IDAM_USER_ID, false);
    }

    @Test
    void claimTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        assertThatThrownBy(() -> taskManagementService.claimTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).claimTask(any(), any());
    }

    @Test
    void claimTask_should_throw_exception_when_missing_required_arguments() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());
        assertThatThrownBy(() -> taskManagementService.claimTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("UserId cannot be null");
    }

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
}

