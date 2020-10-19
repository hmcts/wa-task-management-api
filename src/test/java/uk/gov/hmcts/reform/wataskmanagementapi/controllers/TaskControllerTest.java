package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.apache.commons.lang.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
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

        ResponseEntity<GetTaskResponse<CamundaTask>> response = taskController.getTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetTaskResponse.class));
        assertNotNull(response.getBody());
        assertEquals(mockedTask, response.getBody().getTask());
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
    void should_unclaim_a_task_204_no_content() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";

        ResponseEntity<String> response = taskController.unclaimTask(authToken,taskId);

        assertEquals(HttpStatus.NO_CONTENT,response.getStatusCode());
    }

    @Test
    void should_throw_not_implemented_exception_for_work_in_progress_endpoints() {

        assertThatThrownBy(() -> taskController.searchWithCriteria())
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");

        String someTaskId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> taskController.assignTask(
            someTaskId,
            new AssignTaskRequest("some-user")
        ))
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");
    }

    @Test
    void should_complete_a_task() {
        String taskId = UUID.randomUUID().toString();

        ResponseEntity response = taskController.completeTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
