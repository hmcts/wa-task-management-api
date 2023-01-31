package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ReportableTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class ReportableTaskController {
    private final MIReportingService taskHistoryService;

    public ReportableTaskController(MIReportingService taskHistoryService) {
        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping(
        path = "/task/{task-id}/reportable",
        produces = APPLICATION_JSON_VALUE
    )
    public ReportableTaskResponse getReportableTask(@PathVariable("task-id") String taskId) {
        return ReportableTaskResponse.builder()
            .reportableTaskList(taskHistoryService.findByReportingTaskId(taskId))
            .build();
    }
}
