package uk.gov.hmcts.reform.wataskmanagementapi.poc.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskSecondaryKeyConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.api.TasksApi;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RequiredArgsConstructor
@Slf4j
@RestController

public class TaskCreateController implements TasksApi {

    private final TaskManagementService taskManagementService;
    private final ClientAccessControlService clientAccessControlService;

    @Override
    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource> createTask(
        @RequestHeader(value = "ServiceAuthorization", required = true) String serviceAuthorization,
        @Valid @RequestBody CreateTaskRequest createTaskRequest
    ) {
        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthorization);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        TaskResource savedTask;
        try {
            savedTask = taskManagementService.addTask(createTaskRequest.getTask());
        } catch (TaskSecondaryKeyConflictException ex) {
            return ResponseEntity
                .noContent()
                .cacheControl(CacheControl.noCache())
                .build();
        }

        taskManagementService.updateTaskIndex(savedTask.getTaskId());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }
}
