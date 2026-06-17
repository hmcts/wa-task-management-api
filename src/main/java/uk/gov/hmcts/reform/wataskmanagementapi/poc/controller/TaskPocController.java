package uk.gov.hmcts.reform.wataskmanagementapi.poc.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.BaseController;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.mapper.GetTasksResponseMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.GetTaskResponseItem;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.TaskReconfigureRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.TaskReconfigureResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.TerminateTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RequiredArgsConstructor
@Slf4j
@RestController
public class TaskPocController extends BaseController {

    private final TaskManagementService taskManagementService;
    private final ClientAccessControlService clientAccessControlService;
    private final GetTasksResponseMapper responseMapper;

    @GetMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetTasksResponse> getTasks(
        @NotNull @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @RequestParam(value = "case_id", required = false) String caseId,
        @RequestParam(value = "task_types", required = false) List<String> taskTypes
    ) {
        checkExclusiveAccess(serviceAuthorization);
        validateGetTasksFilters(caseId, taskTypes);

        List<TaskResource> tasks = taskManagementService.getTasks(caseId, taskTypes);
        List<GetTaskResponseItem> taskItems = responseMapper.mapToGetTaskItems(tasks);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTasksResponse(taskItems, (long) taskItems.size()));
    }

    @PostMapping(
        value = "/tasks",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskResource> createTask(
        @NotNull @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody CreateTaskRequest createTaskRequest
    ) {
        checkExclusiveAccess(serviceAuthorization);
        TaskResource savedTask = taskManagementService.addTask(createTaskRequest.getTask());
        taskManagementService.updateTaskIndex(savedTask.getTaskId());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }

    @PostMapping(
        value = "/tasks/terminate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> terminateTasks(
        @NotNull @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody TerminateTasksRequest terminateTasksRequest
    ) {
        checkExclusiveAccess(serviceAuthorization);

        taskManagementService.terminateTasks(terminateTasksRequest);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @PutMapping(
        value = "/tasks/reconfigure",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskReconfigureResponse> reconfigureTasks(
        @NotNull @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody TaskReconfigureRequest taskReconfigureRequest
    ) {
        checkExclusiveAccess(serviceAuthorization);
        TaskReconfigureResponse response = taskManagementService.reconfigureTasks(taskReconfigureRequest);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);

    }

    private void checkExclusiveAccess(String serviceAuthorization) {
        if (!clientAccessControlService.hasExclusiveAccess(serviceAuthorization)) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }
    }

    private void validateGetTasksFilters(String caseId, List<String> taskTypes) {
        if (caseId == null && taskTypes == null) {
            throw new InvalidRequestException("At least one query/filter parameter must be included in the request.");
        }

        if (caseId != null && StringUtils.isBlank(caseId)) {
            throw new InvalidRequestException("case_id cannot be blank");
        }

        if (taskTypes != null && taskTypes.isEmpty()) {
            throw new InvalidRequestException("task_types cannot be empty");
        }

        if (taskTypes != null && taskTypes.stream().anyMatch(StringUtils::isBlank)) {
            throw new InvalidRequestException("task_types list cannot contain a blank item");
        }
    }

}
