package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskHistoryResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class TaskHistoryController {
    private MIReportingService taskHistoryService;

    public TaskHistoryController(MIReportingService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping(
        value = "/task/{task-id}/history",
        produces = APPLICATION_JSON_VALUE
    )

    public TaskHistoryResponse getTaskHistory(@PathVariable("task-id") String taskId) {
        return TaskHistoryResponse.builder()
            .taskHistoryList(taskHistoryService.findByTaskId(taskId))
            .build();
    }
}
