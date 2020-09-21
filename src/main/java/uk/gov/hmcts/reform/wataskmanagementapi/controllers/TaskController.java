package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

@RestController
public class TaskController {

    private final CamundaService camundaService;

    @Autowired
    public TaskController(CamundaService camundaService) {
        this.camundaService = camundaService;
    }

    @GetMapping(path = "/task/{task-id}", produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> getTask(@PathVariable("task-id") String id)  {
        String apiResponse = camundaService.getTask(id);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(apiResponse);
    }

}
