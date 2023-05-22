package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.ReportableTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskHistoryResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RestController
@Slf4j
@Profile("replica | preview")
public class MIReportingTestingController {
    private final MIReportingService miReportingService;

    @Value("${environment}")
    private String environment;

    public MIReportingTestingController(MIReportingService taskHistoryService) {
        this.miReportingService = taskHistoryService;
    }

    @GetMapping(
        path = "/task/{task-id}/history",
        produces = APPLICATION_JSON_VALUE
    )
    public TaskHistoryResponse getTaskHistory(@PathVariable("task-id") String taskId) {

        if (isNonProdEnvironment()) {
            return TaskHistoryResponse.builder()
                .taskHistoryList(miReportingService.findByTaskId(taskId))
                .build();
        } else {
            log.error("MIReporting endpoint not allowed in '{}' environment.", environment);
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }
    }

    @GetMapping(
        path = "/task/{task-id}/reportable",
        produces = APPLICATION_JSON_VALUE
    )
    public ReportableTaskResponse getReportableTask(@PathVariable("task-id") String taskId) {

        if (isNonProdEnvironment()) {
            return ReportableTaskResponse.builder()
                .reportableTaskList(miReportingService.findByReportingTaskId(taskId))
                .build();
        } else {
            log.error("MIReporting endpoint not allowed in '{}' environment.", environment);
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }
    }

    private boolean isNonProdEnvironment() {
        log.info("MIReporting endpoint accessing in '{}' environment ", environment);
        return !"prod".equalsIgnoreCase(environment);
    }

}
