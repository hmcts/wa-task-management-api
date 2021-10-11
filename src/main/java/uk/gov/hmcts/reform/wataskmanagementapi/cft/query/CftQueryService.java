package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CftQueryService {

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
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);

        Sort sort = SortQuery.sortByFields(searchTaskRequest);

        Pageable page = PageRequest.of(firstResult, maxResults, sort);
        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        final List<TaskResource> taskResources = pages.toList();

        return mapToTask(taskResources, pages.getTotalElements());
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
                taskResource.getCaseName(), taskResource.getHasWarnings(), null, null,
                null
            )
        ).collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, totalNumberOfTasks);
    }

}
