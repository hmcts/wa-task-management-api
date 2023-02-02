package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

@ExtendWith({MockitoExtension.class})
class ExclusiveTaskActionsControllerTest {

    private static final String SERVICE_AUTHORIZATION_TOKEN = "SERVICE_AUTHORIZATION_TOKEN";

    @Mock
    private TaskManagementService taskManagementService;
    @Mock
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private IdamTokenGenerator idamTokenGenerator;
    @Mock
    private UserInfo userInfo;

    private ExclusiveTaskActionsController exclusiveTaskActionsController;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        exclusiveTaskActionsController = new ExclusiveTaskActionsController(
            clientAccessControlService,
            taskManagementService
        );
        lenient().when(idamTokenGenerator.generate()).thenReturn("IDAM_SYS_TOKEN");
        lenient().when(idamTokenGenerator.generate()).thenReturn("SYSTEM_BEARER_TOKEN");
        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn("SYSTEM_USER_IDAM_ID");
    }

    @Nested
    class NewInitiateRequest {
        @Test
        void should_succeed_when_initiating_a_task_and_return_201() {

            InitiateTaskRequestMap req = new InitiateTaskRequestMap(
                INITIATION,
                Map.of(
                    TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                    TASK_NAME.value(), "follow Up Overdue Reasons For Appeal"
                )
            );

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);
            when(taskManagementService.initiateTask(taskId, req))
                .thenReturn(createDummyTaskResource(taskId));
            ResponseEntity<TaskResource> response = exclusiveTaskActionsController
                .initiate(SERVICE_AUTHORIZATION_TOKEN, taskId, req);

            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(taskId, response.getBody().getTaskId());
            assertEquals("follow Up Overdue Reasons For Appeal", response.getBody().getTaskName());
            assertEquals("followUpOverdueReasonsForAppeal", response.getBody().getTaskType());
            assertEquals(UNCONFIGURED, response.getBody().getState());
        }

        @Test
        void should_fail_when_initiating_a_task_and_client_not_whitelisted_and_return_403() {

            InitiateTaskRequestMap req = new InitiateTaskRequestMap(
                INITIATION,
                Map.of(
                    TASK_TITLE.value(), "aTaskTitle",
                    TASK_NAME.value(), "aTaskName"
                )
            );

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            assertThatThrownBy(() -> exclusiveTaskActionsController.initiate(SERVICE_AUTHORIZATION_TOKEN, taskId, req))
                .isInstanceOf(GenericForbiddenException.class)
                .hasNoCause()
                .hasMessage("Forbidden: "
                                + "The action could not be completed because the "
                                + "client/user had insufficient rights to a resource.");
        }

    }

    @Test
    void should_succeed_when_terminating_a_task_and_return_204_when_terminate_reason_is_cancelled() {
        TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("cancelled"));

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        ResponseEntity<Void> response = exclusiveTaskActionsController
            .terminateTask(SERVICE_AUTHORIZATION_TOKEN, taskId, req);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(taskManagementService, times(1))
            .terminateTask(taskId, req.getTerminateInfo());
    }

    @Test
    void should_succeed_when_terminating_a_task_and_return_204_when_terminate_reason_is_completed() {
        TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("completed"));

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        ResponseEntity<Void> response = exclusiveTaskActionsController
            .terminateTask(SERVICE_AUTHORIZATION_TOKEN, taskId, req);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_when_terminating_a_task_and_return_204_when_terminate_reason_is_deleted() {
        TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("deleted"));

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        ResponseEntity<Void> response = exclusiveTaskActionsController
            .terminateTask(SERVICE_AUTHORIZATION_TOKEN, taskId, req);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_fail_when_terminating_a_task_and_client_is_not_whitelisted_return_403() {
        TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("cancelled"));

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        assertThatThrownBy(() -> exclusiveTaskActionsController
            .terminateTask(SERVICE_AUTHORIZATION_TOKEN, taskId, req))
            .isInstanceOf(GenericForbiddenException.class)
            .hasNoCause()
            .hasMessage("Forbidden: "
                            + "The action could not be completed because the "
                            + "client/user had insufficient rights to a resource.");

    }

    private TaskResource createDummyTaskResource(String taskId) {
        return new TaskResource(
            taskId,
            "follow Up Overdue Reasons For Appeal",
            "followUpOverdueReasonsForAppeal",
            UNCONFIGURED
        );
    }
}
