package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskOperationControllerTest {

    private static final String SYSTEM_USER_IDAM_ID = "SYSTEM_USER_IDAM_ID";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "SERVICE_AUTHORIZATION_TOKEN";
    @Mock
    private TaskOperationService taskOperationService;
    @Mock
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private IdamTokenGenerator idamTokenGenerator;
    @Mock
    private UserInfo userInfo;

    private TaskOperationController taskReconfigurationController;

    @BeforeEach
    void setUp() {
        taskReconfigurationController = new TaskOperationController(
            taskOperationService,
            clientAccessControlService
        );
        lenient().when(idamTokenGenerator.generate()).thenReturn("SYSTEM_BEARER_TOKEN");
        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn(SYSTEM_USER_IDAM_ID);
    }

    @ParameterizedTest
    @EnumSource(TaskOperationType.class)
    void should_perform_task_operation_with_privileged_access(TaskOperationType operationType) {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        ResponseEntity response = taskReconfigurationController.performOperation(
            SERVICE_AUTHORIZATION_TOKEN,
            taskOperationRequest(operationType)
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @ParameterizedTest
    @EnumSource(TaskOperationType.class)
    void should_not_perform_task_operation_when_no_privileged_access(TaskOperationType operationType) {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        assertThatThrownBy(() -> taskReconfigurationController.performOperation(
            SERVICE_AUTHORIZATION_TOKEN,
            taskOperationRequest(operationType)
        ))
            .isInstanceOf(GenericForbiddenException.class)
            .hasNoCause()
            .hasMessage("Forbidden: "
                        + "The action could not be completed because the "
                        + "client/user had insufficient rights to a resource.");
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType operationName) {
        TaskOperation operation = TaskOperation.builder()
            .type(operationName)
            .runId("run_id1")
            .maxTimeLimit(2)
            .retryWindowHours(120)
            .build();
        return new TaskOperationRequest(operation, taskFilters());
    }

    private List<TaskFilter<?>> taskFilters() {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }
}
