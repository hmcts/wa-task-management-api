package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.TaskConfigurationRoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

@ExtendWith(MockitoExtension.class)
class TaskAutoRoleAssignmentServiceTest {

    @Mock
    private TaskConfigurationRoleAssignmentService roleAssignmentService;

    @Mock
    private TaskConfigurationCamundaService camundaService;

    private TaskAutoAssignmentService taskAutoAssignmentService;

    private TaskToConfigure testTaskToConfigure;

    @BeforeEach
    void setUp() {
        taskAutoAssignmentService = new TaskAutoAssignmentService(roleAssignmentService, camundaService);
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
            .isEqualTo(roleAssignmentResource2.getActorId());


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

        assertNull(result.getAssignee());
        assertEquals(CFTTaskState.UNASSIGNED, result.getState());
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

    private TaskResource createTaskResource() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId"
        );
        return taskResource;
    }

    private TaskResource createTestTaskWithRoleResources(Set<TaskRoleResource> taskRoleResources) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId",
            taskRoleResources
        );
        return taskResource;
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

}
