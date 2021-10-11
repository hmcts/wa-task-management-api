package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;

@Service
public class CftQueryService {

    private final CamundaService camundaService;
    private final TaskResourceRepository taskResourceRepository;

    public CftQueryService(CamundaService camundaService, TaskResourceRepository taskResourceRepository) {
        this.camundaService = camundaService;
        this.taskResourceRepository = taskResourceRepository;
    }

    public GetTasksResponse<Task> getAllTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);

        Sort sort = SortQuery.sortByFields(searchTaskRequest);

        Pageable page = PageRequest.of(firstResult, maxResults, sort);
        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        final List<TaskResource> taskResources = pages.toList();

        return mapToTask(taskResources, pages.getTotalElements());
    }

    public GetTasksCompletableResponse<Task> searchForCompletableTasks(SearchEventAndCase searchEventAndCase ,
                                                AccessControlResponse accessControlResponse,
                                                List<PermissionTypes> permissionsRequired) {

        //Safe-guard against unsupported Jurisdictions and case types.
        if (!"IA".equalsIgnoreCase(searchEventAndCase.getCaseJurisdiction())
            || !"Asylum".equalsIgnoreCase(searchEventAndCase.getCaseType())) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        //1. Evaluate Dmn
        final List<Map<String, CamundaVariable>> evaluateDmnResult = camundaService.evaluateTaskCompletionDmn(
            searchEventAndCase);

        // Collect task types
        List<String> taskTypes = extractTaskTypes(evaluateDmnResult);

        if (taskTypes.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildQueryForCompletable(searchEventAndCase, accessControlResponse, permissionsRequired, taskTypes);

        final List<TaskResource> taskResources = taskResourceRepository.findAll(taskResourceSpecification);

        if (taskResources.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

        return new GetTasksCompletableResponse<>(taskRequiredForEvent, null);
    }

    private GetTasksResponse<Task> mapToTask(List<TaskResource> taskResources, long totalNumberOfTasks) {
        final List<Task> tasks = taskResources.stream().map(taskResource ->
            new Task(taskResource.getTaskId(), taskResource.getTaskName(), taskResource.getTaskType(),
                taskResource.getState().getValue(), taskResource.getTaskSystem().getValue(),
                taskResource.getSecurityClassification().getSecurityClassification(),
                taskResource.getTitle(),
                taskResource.getCreated() == null ? null : taskResource.getCreated().toZonedDateTime(),
                taskResource.getDueDateTime() == null ? null : taskResource.getDueDateTime().toZonedDateTime(),
                taskResource.getAssignee(), taskResource.getAutoAssigned(),
                taskResource.getExecutionTypeCode().getExecutionName(), taskResource.getJurisdiction(),
                taskResource.getRegion(), taskResource.getLocation(), taskResource.getLocationName(),
                taskResource.getCaseTypeId(), taskResource.getCaseId(), taskResource.getRoleCategory(),
                taskResource.getCaseName(), taskResource.getHasWarnings(), null, null)
        ).collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, totalNumberOfTasks);
    }

    private List<String> extractTaskTypes(List<Map<String, CamundaVariable>> evaluateDmnResult) {
        return evaluateDmnResult.stream()
            .filter(result -> result.containsKey(TASK_TYPE.value()))
            .map(result -> camundaService.getVariableValue(result.get(TASK_TYPE.value()), String.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    }

    private boolean isTaskRequired(List<Map<String, CamundaVariable>> evaluateDmnResult, List<String> taskTypes) {
        /*
         * EvaluateDmnResult contains with and without empty rows for an event.
         * TaskTypes are extracted from evaluateDmnResult.
         * If both the sizes are equal, it means there is no empty row and task is required for the event
         * If they are of different sizes, it means there is an empty row and task is not required
         */
        return evaluateDmnResult.size() == taskTypes.size();
    }

}
