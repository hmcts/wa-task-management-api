package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RequestMapping(
    path = "/task",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
@RestController
public class TaskController {

    private final CamundaService camundaService;

    @Autowired
    public TaskController(CamundaService camundaService) {
        this.camundaService = camundaService;
    }

    @PostMapping
    public ResponseEntity<String> searchWithCriteria() {
        throw new NotImplementedException();
    }

    @GetMapping(path = "/{task-id}")
    public ResponseEntity<String> getTask(@PathVariable("task-id") String id) {
        String apiResponse = camundaService.getTask(id);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(apiResponse);
    }


    @PostMapping(path = "/{task-id}/claim")
    public ResponseEntity<String> claimTask(@PathVariable("task-id") String taskId) {

        //TODO Remove: this demo user as user id will come from JWT token
        String demoUserId = "demo-user";

        camundaService.claimTask(taskId, demoUserId);
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
