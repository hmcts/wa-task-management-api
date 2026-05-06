package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

// TEMPORARY SONAR FIX

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskInitiationPushService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskInitiationPushControllerTest {

    @Mock
    private ClientAccessControlService clientAccessControlService;

    @Mock
    private TaskInitiationPushService taskInitiationPushService;

    @Mock
    private CamundaTaskInitiationRequest request;

    @Mock
    private TaskResource taskResource;

    @InjectMocks
    private TaskInitiationPushController controller;

    @Test
    void should_return_created_response_when_access_allowed() {
        when(clientAccessControlService.hasExclusiveAccess("s2s")).thenReturn(true);
        when(request.getProcessInstanceId()).thenReturn("process-1");
        when(taskInitiationPushService.initiateTask("task-1", request)).thenReturn(taskResource);

        ResponseEntity<TaskResource> response = controller.initiate("s2s", "task-1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(taskResource, response.getBody());
    }

    @Test
    void should_throw_forbidden_when_access_denied() {
        when(clientAccessControlService.hasExclusiveAccess("s2s")).thenReturn(false);

        assertThrows(GenericForbiddenException.class, () -> controller.initiate("s2s", "task-1", request));
        verifyNoInteractions(taskInitiationPushService);
    }
}
