package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskAssignmentsResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class TaskAssignmentsController {
    private final MIReportingService taskAssignmentsService;

    public TaskAssignmentsController(MIReportingService taskAssignmentsService) {
        this.taskAssignmentsService = taskAssignmentsService;
    }

    @GetMapping(
        path = "/task/{task-id}/assignments",
        produces = APPLICATION_JSON_VALUE
    )
    public TaskAssignmentsResponse getTaskHistory(@PathVariable("task-id") String taskId) {
        return TaskAssignmentsResponse.builder()
            .taskAssignmentsList(taskAssignmentsService.findByAssignmentsTaskId(taskId))
            .build();
    }
}
