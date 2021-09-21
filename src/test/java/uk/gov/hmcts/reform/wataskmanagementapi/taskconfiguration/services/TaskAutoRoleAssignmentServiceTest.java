package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.TaskConfigurationRoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
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

}
