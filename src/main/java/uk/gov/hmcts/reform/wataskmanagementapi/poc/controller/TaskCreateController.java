package uk.gov.hmcts.reform.wataskmanagementapi.poc.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.api.TaskApi;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TaskCreateController implements TaskApi {

    private final TaskManagementService taskManagementService;
    private final ClientAccessControlService clientAccessControlService;

    @Override
    public ResponseEntity<uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource> createTask(String serviceAuthorization, CreateTaskRequest createTaskRequest) {
        log.info("Received request to create task with payload: {}", createTaskRequest);

        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthorization);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        TaskResource savedTask = taskManagementService.addTask(createTaskRequest.getTask());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }
}
