package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RequestMapping(
    path = "/task",
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RestController
public class TaskController {

    private final CamundaService camundaService;
    private final IdamService idamService;

    @Autowired
    public TaskController(CamundaService camundaService, IdamService idamService) {
        this.camundaService = camundaService;
        this.idamService = idamService;
    }

    @PostMapping
    public ResponseEntity<String> searchWithCriteria() {
        throw new NotImplementedException();
    }

    @GetMapping(path = "/{task-id}")
    public ResponseEntity<CamundaTask> getTask(@PathVariable("task-id") String id) {
        CamundaTask apiResponse = camundaService.getTask(id);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(apiResponse);
    }


    @PostMapping(path = "/{task-id}/claim")
    public ResponseEntity<String> claimTask(@RequestHeader("Authorization") String authToken,
                                            @PathVariable("task-id") String taskId) {
        String userId = idamService.getUserId(authToken);
        camundaService.claimTask(taskId, userId);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();

    }

    @PostMapping(path = "/{task-id}/unclaim")
    public ResponseEntity<String> unclaimTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }

    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<String> assignTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }

    @PostMapping(path = "/{task-id}/complete")
    public ResponseEntity<String> completeTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }
}
