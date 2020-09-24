package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

        when(camundaService.getTask(taskId)).thenReturn("aTask");

        ResponseEntity<String> response = taskController.getTask(taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(
            response.getBody(),
            containsString("aTask")
        );
    }

    @Test
    public void should_throw_not_implemented_exception_for_work_in_progress_endpoints() {

        assertThatThrownBy(() -> taskController.searchWithCriteria())
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");

        String someTaskId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> taskController.claimTask(someTaskId))
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");

        assertThatThrownBy(() -> taskController.unclaimTask(someTaskId))
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");

        assertThatThrownBy(() -> taskController.assignTask(someTaskId))
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");

        assertThatThrownBy(() -> taskController.completeTask(someTaskId))
            .isInstanceOf(NotImplementedException.class)
            .hasMessage("Code is not implemented");
    }
}
