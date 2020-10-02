package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.apache.commons.lang.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private CamundaService camundaService;

    @Mock
    private IdamService idamService;

    private TaskController taskController;

    @BeforeEach
    void setUp() {
        taskController = new TaskController(camundaService, idamService);
    }

    @Test
    void should_return_a_fetched_task() {

        String taskId = UUID.randomUUID().toString();

        CamundaTask mockedTask = mock(CamundaTask.class);
        when(camundaService.getTask(taskId)).thenReturn(mockedTask);

        ResponseEntity<CamundaTask> response = taskController.getTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockedTask, response.getBody());
    }

    @Test
    void should_succeed_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";
        String userId = UUID.randomUUID().toString();

        when(idamService.getUserId(authToken)).thenReturn(userId);

        ResponseEntity<String> response = taskController.claimTask(authToken, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_throw_a_non_implemented_exception_and_return_500() {

        String taskId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> taskController.searchWithCriteria())
            .isInstanceOf(NotImplementedException.class)
            .hasNoCause();

        assertThatThrownBy(() -> taskController.unclaimTask(taskId))
            .isInstanceOf(NotImplementedException.class)
            .hasNoCause();

        assertThatThrownBy(() -> taskController.assignTask(taskId))
            .isInstanceOf(NotImplementedException.class)
            .hasNoCause();

        assertThatThrownBy(() -> taskController.completeTask(taskId))
            .isInstanceOf(NotImplementedException.class)
            .hasNoCause();
    }
}
