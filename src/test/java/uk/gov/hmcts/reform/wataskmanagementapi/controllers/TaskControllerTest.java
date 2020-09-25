package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TaskControllerTest {

    @Mock
    private CamundaService camundaService;

    private TaskController taskController;

    @Before
    public void setUp() {
        taskController = new TaskController(camundaService);
    }

    @Test
    public void should_return_a_fetched_task() {

        String taskId = UUID.randomUUID().toString();

        CamundaTask mockedTask = mock(CamundaTask.class);
        when(camundaService.getTask(taskId)).thenReturn(mockedTask);

        ResponseEntity<CamundaTask> response = taskController.getTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockedTask, response.getBody());
    }


    @Test
    public void should_succeed_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();

        ResponseEntity<String> response = taskController.claimTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
