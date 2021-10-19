package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
@Service
public class CftQueryService {
    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management");

    private final TaskResourceRepository taskResourceRepository;

    public CftQueryService(TaskResourceRepository taskResourceRepository) {
        this.taskResourceRepository = taskResourceRepository;
    }

    public GetTasksResponse<Task> getAllTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {

        validateRequest(searchTaskRequest);
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);

        Sort sort = SortQuery.sortByFields(searchTaskRequest);

        Pageable page = PageRequest.of(firstResult, maxResults, sort);
        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        final List<TaskResource> taskResources = pages.toList();

        return mapToTask(taskResources, pages.getTotalElements());
    }

    private void validateRequest(SearchTaskRequest searchTaskRequest) {
        List<Violation> violations = new ArrayList<>();

        //Validate work-type
        List<SearchParameter> workType = searchTaskRequest.getSearchParameters().stream()
            .filter(sp -> sp.getKey().equals(SearchParameterKey.WORK_TYPE))
            .collect(Collectors.toList());

        if (!workType.isEmpty()) {
            //validate work type
            SearchParameter workTypeParameter = workType.get(0);
            List<String> values = workTypeParameter.getValues();
            //Validate
            values.forEach(value -> {
                if (!ALLOWED_WORK_TYPES.contains(value)) {
                    violations.add(new Violation(
                        value,
                        workTypeParameter.getKey() + " must be one of " + Arrays.toString(ALLOWED_WORK_TYPES.toArray())
                    ));
                }
            });
        }

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
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

}
