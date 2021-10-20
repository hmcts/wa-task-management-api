package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CftQueryService {

    private final CFTTaskMapper cftTaskMapper;
    private final TaskResourceRepository taskResourceRepository;

    public CftQueryService(CFTTaskMapper cftTaskMapper, TaskResourceRepository taskResourceRepository) {
        this.cftTaskMapper = cftTaskMapper;
        this.taskResourceRepository = taskResourceRepository;
    }

    public GetTasksResponse<Task> getAllTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {
        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        Pageable page;
        try {
            page = PageRequest.of(firstResult, maxResults, sort);
        } catch (IllegalArgumentException exp) {
            log.debug(String.format("remove here getAllTasks exception : %s", exp.getMessage()));
            return new GetTasksResponse<>(Collections.emptyList(), 0);
        }

        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);

        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        final List<TaskResource> taskResources = pages.toList();

        return mapToTask(taskResources, pages.getTotalElements());
    }

    private GetTasksResponse<Task> mapToTask(List<TaskResource> taskResources, long totalNumberOfTasks) {
        final List<Task> tasks = taskResources.stream().map(cftTaskMapper::mapToTask
        ).collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, totalNumberOfTasks);
    }

    public Optional<TaskResource> getTask(String taskId,
                                          AccessControlResponse accessControlResponse,
                                          List<PermissionTypes> permissionsRequired
    ) {

        if (permissionsRequired.isEmpty()
            || taskId == null
            || taskId.isBlank()) {
            return Optional.empty();
        }
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildSingleTaskQuery(taskId, accessControlResponse, permissionsRequired);

        return taskResourceRepository.findOne(taskResourceSpecification);

    }
}
