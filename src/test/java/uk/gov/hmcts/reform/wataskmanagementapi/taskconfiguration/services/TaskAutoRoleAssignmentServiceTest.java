package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.TaskConfigurationRoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

@ExtendWith(MockitoExtension.class)
class TaskAutoRoleAssignmentServiceTest {

    @Mock
    private TaskConfigurationRoleAssignmentService roleAssignmentService;

    @Mock
    private TaskConfigurationCamundaService camundaService;

    @Mock
    private CftQueryService cftQueryService;

    private TaskAutoAssignmentService taskAutoAssignmentService;

    private TaskToConfigure testTaskToConfigure;

    @BeforeEach
    void setUp() {
        taskAutoAssignmentService = new TaskAutoAssignmentService(
            roleAssignmentService,
            camundaService,
            cftQueryService
        );
        testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            "someCaseId",
            "taskName"
        );

    }

    @Test
    void getAutoAssignmentVariables_should_return_unassigned_task_state_and_null_assignee() {

        when(roleAssignmentService.searchRolesByCaseId(testTaskToConfigure.getCaseId()))
            .thenReturn(emptyList());


        AutoAssignmentResult result = taskAutoAssignmentService.getAutoAssignmentVariables(testTaskToConfigure);

        assertThat(result.getAssignee()).isNull();
        assertThat(result.getTaskState()).isEqualTo(UNASSIGNED.value());

    }

    @Test
    void getAutoAssignmentVariables_should_return_assigned_task_state_and_assignee() {

        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();

        when(roleAssignmentService.searchRolesByCaseId(testTaskToConfigure.getCaseId()))
            .thenReturn(singletonList(roleAssignmentResource));


        AutoAssignmentResult result = taskAutoAssignmentService.getAutoAssignmentVariables(testTaskToConfigure);

        assertThat(result.getAssignee()).isEqualTo("someUserId");
        assertThat(result.getTaskState()).isEqualTo(ASSIGNED.value());
    }

    @Test
    void autoAssignTask_should_update_task_state_only_to_unassigned() {

        when(roleAssignmentService.searchRolesByCaseId(testTaskToConfigure.getCaseId()))
            .thenReturn(emptyList());

        taskAutoAssignmentService.autoAssignTask(testTaskToConfigure, UNCONFIGURED.value());

        verify(camundaService).updateTaskStateTo(
            testTaskToConfigure.getId(),
            UNASSIGNED
        );
    }

    @Test
    void autoAssignTask_should_update_auto_assign() {
        RoleAssignment roleAssignmentResource = RoleAssignment.builder()
            .id("someId")
            .actorIdType(ActorIdType.IDAM)
            .actorId("someUserId")
            .roleName("tribunal-caseworker")
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .build();

        when(roleAssignmentService.searchRolesByCaseId(testTaskToConfigure.getCaseId()))
            .thenReturn(singletonList(roleAssignmentResource));

        taskAutoAssignmentService.autoAssignTask(testTaskToConfigure, UNCONFIGURED.value());

        verify(camundaService).assignTask(
            testTaskToConfigure.getId(),
            "someUserId",
            UNCONFIGURED.value()
        );
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

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

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

    @Test
    void auto_assign_should_return_task_assignee_when_auto_assignable_is_true_and_authorisation_is_null() {
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
            taskRoleResource("tribunal-caseworker", true, 3, null),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{})
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
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        //first role assignment set
        RoleAssignment roleAssignmentResource1 = createRoleAssignment(
            "someuser@test.com",
            "tribunal-caseworker",
            List.of("IA", "DIVORCE", "PROBATE")
        );

        roleAssignments.add(roleAssignmentResource1);
        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource1);
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
        taskResource.setState(CFTTaskState.UNASSIGNED);

        TaskResource autoAssignCFTTaskResponse =
            taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.UNASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(null);
    }

    @Test
    void auto_assign_should_return_task_assignee_when_auto_assignable_is_true_and_authorisation_is_empty() {
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
            taskRoleResource("tribunal-caseworker", true, 3, new String[]{}),
            taskRoleResource("senior-tribunal-caseworker", false, 2, new String[]{})
        );
        taskResource.setTaskRoleResources(taskRoleResources);

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(roleAssignments);

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals(CFTTaskState.ASSIGNED, autoAssignCFTTaskResponse.getState());

        assertThat(autoAssignCFTTaskResponse.getAssignee())
            .isEqualTo(roleAssignmentResource.getActorId());

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

        TaskResource autoAssignCFTTaskResponse = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

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
    void autoAssignCFTTask_should_update_when_task_role_resources_available_and_auto_assignable_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            false
        );

        TaskResource taskResource = createTestTaskWithRoleResources(singleton(taskRoleResource));

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
    void autoAssignCFTTask_should_update_when_task_role_resources_available_and_auto_assignable_true_no_match() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );

        TaskResource taskResource = createTestTaskWithRoleResources(singleton(taskRoleResource));

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

        assertThat(result.getAssignee())
            .isEqualTo(roleAssignmentResource.getActorId());
        assertEquals(CFTTaskState.ASSIGNED, result.getState());
    }


    @Test
    void autoAssignCFTTask_should_update_when_task_role_resources_available_and_auto_assignable_true_and_match() {

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

        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource))
            .thenReturn(singletonList(roleAssignmentResource));

        TaskResource result = taskAutoAssignmentService.autoAssignCFTTask(taskResource);

        assertEquals("someUserId", result.getAssignee());
        assertEquals(CFTTaskState.ASSIGNED, result.getState());
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
