package uk.gov.hmcts.reform.wataskmanagementapi.services;

// TEMPORARY SONAR FIX

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;

@ExtendWith(MockitoExtension.class)
class TaskInitiationPushServiceTest {

    @Mock
    private CamundaTaskInitiationRequestMapper requestMapper;

    @Mock
    private TaskManagementService taskManagementService;

    @Mock
    private TaskResource taskResource;

    @InjectMocks
    private TaskInitiationPushService service;

    @Test
    void should_initiate_task_when_unconfigured() {
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            "name",
            null,
            null,
            null,
            null,
            "process-1",
            Map.of(CFT_TASK_STATE.value(), new CamundaVariable("unconfigured", "String"))
        );
        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, Map.of());

        when(requestMapper.map("task-1", request)).thenReturn(initiateTaskRequest);
        when(taskManagementService.initiateTask("task-1", initiateTaskRequest)).thenReturn(taskResource);
        when(taskResource.getTaskId()).thenReturn("task-1");

        TaskResource result = service.initiateTask("task-1", request);

        assertSame(taskResource, result);
        verify(taskManagementService).updateTaskIndex("task-1");
    }

    @Test
    void should_throw_when_task_not_unconfigured() {
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            "name",
            null,
            null,
            null,
            null,
            "process-1",
            Map.of(CFT_TASK_STATE.value(), new CamundaVariable("assigned", "String"))
        );

        assertThrows(IllegalArgumentException.class, () -> service.initiateTask("task-1", request));
    }
}
