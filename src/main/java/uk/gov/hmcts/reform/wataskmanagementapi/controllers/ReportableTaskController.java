package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ReportableTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Profile("replica | preview")
public class ReportableTaskController {
    private final MIReportingService taskReportingService;

    public ReportableTaskController(MIReportingService taskHistoryService) {
        this.taskReportingService = taskHistoryService;
    }

    @GetMapping(
        path = "/task/{task-id}/reportable",
        produces = APPLICATION_JSON_VALUE
    )
    public ReportableTaskResponse getReportableTask(@PathVariable("task-id") String taskId) {
        return ReportableTaskResponse.builder()
            .reportableTaskList(taskReportingService.findByReportingTaskId(taskId))
            .build();
    }
}
