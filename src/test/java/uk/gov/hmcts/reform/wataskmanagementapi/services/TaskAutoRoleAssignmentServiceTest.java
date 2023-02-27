package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class TaskAutoRoleAssignmentServiceTest {

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private CftQueryService cftQueryService;

    @Mock
    private IdamTokenGenerator idamTokenGenerator;

    @Mock
    private UserInfo userInfo;

    private TaskAutoAssignmentService taskAutoAssignmentService;

    private TaskToConfigure testTaskToConfigure;

    @BeforeEach
    void setUp() {
        taskAutoAssignmentService = new TaskAutoAssignmentService(
            roleAssignmentService,
            cftQueryService,
            idamTokenGenerator
        );
        testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            "someCaseId",
            "taskName"
        );

        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn("IDAM_SYSTEM_USER");
    }

    @Test
    void autoAssignCFTTask_should_update_when_no_role_assignments() {
        TaskResource taskResource = createTaskResource();

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(emptyList());


        TaskResource result = taskAutoAssignmentService.autoAssignCFTTask(taskResource);


        assertNull(result.getAssignee());
        assertEquals(CFTTaskState.UNASSIGNED, result.getState());
    }

    @Test
    void autoAssignCFTTask_should_update_when_role_assignment_with_no_name() {
        TaskResource taskResource = createTaskResource();

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", false, 1, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();

        roleAssignments.add(roleAssignmentResource);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource result = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertNull(result.getAssignee());
        assertEquals(CFTTaskState.UNASSIGNED, result.getState());
    }

    @Test
    void autoAssignCFTTask_should_update_when_no_task_role_resource() {
        TaskResource taskResource = createTaskResource();
        taskResource.setAssignee("someUser");
        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(singletonList(roleAssignmentResource));

        TaskResource result = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertNull(result.getAssignee());
        assertEquals(CFTTaskState.UNASSIGNED, result.getState());
    }

    @Test
    void should_assign_task_to_highest_priority_user_when_more_than_one_role_assignments_set_to_a_task() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2),
            taskRoleResource("senior-tribunal-caseworker", false, 1)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService
            .autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());
        assertNotNull(autoAssignCFTTaskResponse.getAssignee());
        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());


        //set tribunal-caseworker high prioritised user
        taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2),
            taskRoleResource("senior-tribunal-caseworker", false, 3)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());

        //set tribunal-caseworker high prioritised user
        taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 3),
            taskRoleResource("senior-tribunal-caseworker", true, 2)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource2.getActorId());

    }

    //autoassign true, task unassigned
    @Test
    void auto_assign_should_assign_when_task_is_unassigned_auto_assign_true_and_match_authorisations() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA", "DIVORCE", "PROBATE"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA", "DIVORCE", "PROBATE"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService
            .autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_true_and_not_matching_authorisation() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_assign_when_task_is_unassigned_auto_assign_true_and_authorisation_is_null() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, null),
            taskRoleResource("senior-tribunal-caseworker", true, 2, null)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void auto_assign_should_assign_when_task_is_unassigned_auto_assign_true_and_authorisation_is_empty() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE")
        );
        roleAssignments.add(roleAssignmentResource);

        //Authorisations is empty
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService
            .autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource.getActorId());

    }

    @Test
    void auto_assign_should_not_assign_when_task_unassigned_auto_assign_true_and_role_assignment_with_null_authns() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            null
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            null
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    //autoassign true, task assigned
    @Test
    void auto_assign_should_assign_when_task_is_assigned_auto_assign_true_and_match_authorisations() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA", "DIVORCE", "PROBATE"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA", "DIVORCE", "PROBATE"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_assigned_auto_assign_true_and_not_matching_authorisation() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_assign_when_task_is_assigned_auto_assign_true_and_authorisation_is_null() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, null),
            taskRoleResource("senior-tribunal-caseworker", true, 2, null)
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void auto_assign_should_assign_when_task_is_assigned_auto_assign_is_true_and_authorisation_is_empty() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        //Authorisations is empty
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());

    }

    @Test
    void auto_assign_should_not_assign_when_task_is_assigned_auto_assign_true_and_role_assignment_null_authns() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            null
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            null
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", true, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    //autoassign false, task unassigned
    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_false_and_match_authorisations() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA", "DIVORCE", "PROBATE"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA", "DIVORCE", "PROBATE"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_false_and_not_matching_authorisation() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_false_and_authorisation_is_null() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, null),
            taskRoleResource("senior-tribunal-caseworker", false, 2, null)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_false_and_authorisation_is_empty() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE")
        );
        roleAssignments.add(roleAssignmentResource);

        //Authorisations is empty
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());

    }

    @Test
    void auto_assign_should_not_assign_when_task_is_unassigned_auto_assign_false_and_role_assignment_null_authns() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            null
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            null
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    //autoassign false, task assigned
    @Test
    void auto_assign_should_assign_when_task_is_assigned_auto_assign_false_and_match_authorisations() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA", "DIVORCE", "PROBATE"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA", "DIVORCE", "PROBATE"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void auto_assign_should_not_assign_when_task_is_assigned_auto_assign_false_and_not_matching_authorisation() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_unassign_when_task_is_assigned_auto_assign_false_and_authorisation_is_null() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, null),
            taskRoleResource("senior-tribunal-caseworker", false, 2, null)
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void auto_assign_should_unassign_when_task_is_assigned_auto_assign_false_and_authorisation_is_empty() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        //Authorisations is empty
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());

    }

    @Test
    void auto_assign_should_not_assign_when_task_is_assigned_auto_assign_false_and_role_assignment_null_authns() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            null
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "highPrioritisedUser",
            "senior-tribunal-caseworker",
            null
        );
        roleAssignments.add(roleAssignmentResource2);

        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", false, 1, new String[]{"IA"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{"IA"})
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
    }

    @Test
    void should_return_task_assignee_null_when_authorisation_is_different() {
        TaskResource taskResource = createTaskResource();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource = createRoleAssignment(
            "lowPrioritisedUser",
            "tribunal-caseworker",
            List.of("DIVORCE")
        );
        roleAssignments.add(roleAssignmentResource);

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2)
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setAssignee("dummyUser");
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService
            .autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());
        assertNull(autoAssignCFTTaskResponse.getAssignee());


        //Authorisations don't match
        taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 3, new String[]{"Test"}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());

        assertNull(autoAssignCFTTaskResponse.getAssignee());


    }

    @Test
    void autoAssignCFTTask_should_update_when_task_role_resource_is_empty() {
        TaskResource taskResource = createTestTaskWithRoleResources(emptySet());

        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(singletonList(roleAssignmentResource));

        TaskResource result = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertNull(result.getAssignee());
        assertEquals(CFTTaskState.UNASSIGNED, result.getState());
    }

    @Test
    void should_reassign_task_to_other_user_when_current_user_does_not_have_own_execute_permissions() {

        RoleAssignment roleAssignmentResource = createRoleAssignment(
            "someinvalidUser@test.com",
            "case-worker",
            singletonList("IA")
        );

        TaskResource taskResource = createTaskResource();
        taskResource.setAssignee(roleAssignmentResource.getActorId());

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "someuser@test.com",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);

        //second role assignment set
        RoleAssignment roleAssignmentResource2 = createRoleAssignment(
            "someotheruser@test.com",
            "senior-tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );
        roleAssignments.add(roleAssignmentResource2);

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2),
            taskRoleResource("senior-tribunal-caseworker", true, 1),
            taskRoleResource("caseworker", true, 3)
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource);
        when(roleAssignmentService.getRolesByUserId(taskResource.getAssignee()))
            .thenReturn(roleAssignmentForAssignee);
        when(cftQueryService.getTask(taskResource.getTaskId(), roleAssignmentForAssignee, List.of(OWN, EXECUTE)))
            .thenReturn(Optional.empty());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse =
            taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource2.getActorId());
    }

    @Test
    void should_not_reassign_task_to_other_user_when_current_user_have_own_execute_permissions() {

        TaskResource taskResource = createTaskResource();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "someuser@test.com",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2),
            taskRoleResource("senior-tribunal-caseworker", true, 1),
            taskRoleResource("caseworker", true, 3)
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setAssignee(roleAssignmentResource1.getActorId());
        taskResource.setState(CFTTaskState.ASSIGNED);

        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource1);
        when(roleAssignmentService.getRolesByUserId(taskResource.getAssignee()))
            .thenReturn(roleAssignmentForAssignee);
        when(cftQueryService.getTask(taskResource.getTaskId(), roleAssignmentForAssignee, List.of(OWN, EXECUTE)))
            .thenReturn(Optional.of(taskResource));

        TaskResource autoAssignCFTTaskResponse =
            taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource1.getActorId());
    }

    @Test
    void should_not_reassign_task_when_task_is_unassigned() {

        TaskResource taskResource = createTaskResource();

        //set senior-tribunal-caseworker high prioritised user
        Set<TaskRoleResource> taskRoleResources = Set.of(
            taskRoleResource("tribunal-caseworker", true, 2),
            taskRoleResource("senior-tribunal-caseworker", true, 1),
            taskRoleResource("caseworker", true, 3)
        );
        taskResource.setTaskRoleResources(taskRoleResources);
        taskResource.setState(CFTTaskState.UNASSIGNED);

        TaskResource autoAssignCFTTaskResponse =
            taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isNull();
    }

    @Test
    void checkAssigneeIsStillValid_should_return_true_and_match() {
        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(singletonList("IA"))
            .build();

        when(roleAssignmentService.getRolesByUserId(any())).thenReturn(singletonList(roleAssignmentResource));

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            0,
            true
        );
        TaskResource taskResource = createTestTaskWithRoleResources(singleton(taskRoleResource));

        boolean result = taskAutoAssignmentService.checkAssigneeIsStillValid(taskResource, "someUserId");

        assertTrue(result);
    }

    @ParameterizedTest(name = ARGUMENTS_WITH_NAMES_PLACEHOLDER)
    @MethodSource("checkAssigneeIsStillValidNegativeScenarioProvider")
    void checkAssigneeIsStillValid_should_return_false(String testName, CheckAssigneeScenario scenario) {
        when(roleAssignmentService.getRolesByUserId(scenario.userId)).thenReturn(scenario.roleAssignments);
        TaskResource taskResource = createTestTaskWithRoleResources(singleton(scenario.taskRoleResource));
        assertFalse(taskAutoAssignmentService.checkAssigneeIsStillValid(taskResource, scenario.userId));
    }

    @ParameterizedTest(name = ARGUMENTS_WITH_NAMES_PLACEHOLDER)
    @MethodSource("checkAssigneeIsStillValidPositiveScenarioProvider")
    void checkAssigneeIsStillValid_should_return_true(String testName, CheckAssigneeScenario scenario) {
        when(roleAssignmentService.getRolesByUserId(scenario.userId)).thenReturn(scenario.roleAssignments);
        TaskResource taskResource = createTestTaskWithRoleResources(singleton(scenario.taskRoleResource));
        assertTrue(taskAutoAssignmentService.checkAssigneeIsStillValid(taskResource, scenario.userId));
    }

    private TaskResource createTaskResource() {
        return new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId"
        );
    }

    private TaskResource createTestTaskWithRoleResources(Set<TaskRoleResource> taskRoleResources) {
        return new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId",
            taskRoleResources
        );
    }

    private TaskRoleResource taskRoleResource(String name, boolean autoAssign, int assignmentPriority) {
        return new TaskRoleResource(
            name,
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            assignmentPriority,
            autoAssign
        );
    }

    private TaskRoleResource taskRoleResource(String name, boolean autoAssign,
                                              int assignmentPriority,
                                              String[] authorisations) {
        return new TaskRoleResource(
            name,
            true,
            true,
            false,
            true,
            true,
            true,
            authorisations,
            assignmentPriority,
            autoAssign
        );
    }

    private RoleAssignment createRoleAssignment(String actorId, String roleName, List<String> authorisations) {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId(actorId)
            .roleName(roleName)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(authorisations)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
    }

    private static Stream<Arguments> checkAssigneeIsStillValidNegativeScenarioProvider() {

        TaskRoleResource emptyTaskRoleResourceAuthorization = getBaseTaskRoleResource(false);
        emptyTaskRoleResourceAuthorization.setAuthorizations(new String[]{});


        TaskRoleResource nullTaskRoleResourceAuthorization = getBaseTaskRoleResource(false);
        nullTaskRoleResourceAuthorization.setAuthorizations(null);

        TaskRoleResource notAutoAssignableTaskRoleResourceAuthorization = getBaseTaskRoleResource(true);
        notAutoAssignableTaskRoleResourceAuthorization.setAuthorizations(new String[]{"Test"});


        CheckAssigneeScenario nullTaskRoleResourceAuthorizationScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(getBaseRoleAssignment()))
            .taskRoleResource(nullTaskRoleResourceAuthorization)
            .userId("someUserId")
            .build();

        CheckAssigneeScenario notAutoAssignableTaskRoleResourceScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(getBaseRoleAssignment()))
            .taskRoleResource(notAutoAssignableTaskRoleResourceAuthorization)
            .userId("someUserId")
            .build();

        CheckAssigneeScenario emptyTaskRoleResourceAuthorizationScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(getBaseRoleAssignment()))
            .taskRoleResource(emptyTaskRoleResourceAuthorization)
            .userId("someUserId")
            .build();

        CheckAssigneeScenario noRoleAssignmentScenario = CheckAssigneeScenario.builder()
            .roleAssignments(emptyList())
            .taskRoleResource(getBaseTaskRoleResource(false))
            .userId("someUserId")
            .build();

        RoleAssignment roleAssignmentNoAuthorizations = getBaseRoleAssignment();
        roleAssignmentNoAuthorizations.setAuthorisations(emptyList());

        CheckAssigneeScenario noRoleAssignmentAuthorizationsScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(roleAssignmentNoAuthorizations))
            .taskRoleResource(getBaseTaskRoleResource(false))
            .userId("someUserId")
            .build();

        return Stream.of(
            Arguments.of("Empty Task Role Resource Authorizations", emptyTaskRoleResourceAuthorizationScenario),
            Arguments.of("Not Auto Assignable Task Role Resource", notAutoAssignableTaskRoleResourceScenario),
            Arguments.of("Null Task Role Resource Authorizations", nullTaskRoleResourceAuthorizationScenario),
            Arguments.of("No Role Assignment", noRoleAssignmentScenario),
            Arguments.of("No Role Assignment Authorizations", noRoleAssignmentAuthorizationsScenario)
        );
    }

    private static Stream<Arguments> checkAssigneeIsStillValidPositiveScenarioProvider() {

        TaskRoleResource emptyTaskRoleResourceAuthorization = getBaseTaskRoleResource(true);
        emptyTaskRoleResourceAuthorization.setAuthorizations(new String[]{});

        CheckAssigneeScenario emptyTaskRoleResourceAuthorizationScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(getBaseRoleAssignment()))
            .taskRoleResource(emptyTaskRoleResourceAuthorization)
            .userId("someUserId")
            .build();

        TaskRoleResource nullTaskRoleResourceAuthorization = getBaseTaskRoleResource(true);
        nullTaskRoleResourceAuthorization.setAuthorizations(null);

        CheckAssigneeScenario nullTaskRoleResourceAuthorizationScenario = CheckAssigneeScenario.builder()
            .roleAssignments(singletonList(getBaseRoleAssignment()))
            .taskRoleResource(nullTaskRoleResourceAuthorization)
            .userId("someUserId")
            .build();

        return Stream.of(
            Arguments.of("Empty Task Role Resource Authorizations", emptyTaskRoleResourceAuthorizationScenario),
            Arguments.of("Null Task Role Resource Authorizations", nullTaskRoleResourceAuthorizationScenario)
        );
    }

    private static RoleAssignment getBaseRoleAssignment() {
        return RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(singletonList("IA"))
            .build();
    }

    private static TaskRoleResource getBaseTaskRoleResource(boolean autoAssignable) {
        return new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            0,
            autoAssignable
        );
    }


    @Builder
    private static class CheckAssigneeScenario {
        List<RoleAssignment> roleAssignments;
        TaskRoleResource taskRoleResource;
        String userId;
    }

}
