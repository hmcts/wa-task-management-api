package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskReconfigurationControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "SERVICE_AUTHORIZATION_TOKEN";
    @Mock
    private TaskManagementService taskManagementService;
    @Mock
    private ClientAccessControlService clientAccessControlService;

    private TaskReconfigurationController taskReconfigurationController;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        taskReconfigurationController = new TaskReconfigurationController(
            taskManagementService,
            clientAccessControlService
        );

    }

    @Test
    void should_perform_task_operation_mark_reconfigure_with_privileged_access() {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        ResponseEntity response = taskReconfigurationController.performOperation(
            SERVICE_AUTHORIZATION_TOKEN,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_not_perform_task_operation_mark_reconfigure_when_no_privileged_access() {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        assertThatThrownBy(() -> taskReconfigurationController.performOperation(
            SERVICE_AUTHORIZATION_TOKEN,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
            .isInstanceOf(GenericForbiddenException.class)
            .hasNoCause()
            .hasMessage("Forbidden: "
                        + "The action could not be completed because the "
                        + "client/user had insufficient rights to a resource.");
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName) {
        TaskOperation operation = new TaskOperation(operationName, "run_id1", 2, 120);
        return new TaskOperationRequest(operation, taskFilters());
    }

    private List<TaskFilter<?>> taskFilters() {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }
}
