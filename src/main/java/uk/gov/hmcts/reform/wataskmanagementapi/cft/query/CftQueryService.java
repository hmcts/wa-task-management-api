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

import java.util.List;

@Service
public class CftQueryService {

    private final TaskResourceRepository taskResourceRepository;

    public CftQueryService(TaskResourceRepository taskResourceRepository) {
        this.taskResourceRepository = taskResourceRepository;
    }

    public List<TaskResource> getAllTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
         List<PermissionTypes> permissionsRequired
    ) {
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .getTasks(searchTaskRequest, accessControlResponse, permissionsRequired);

        Sort sort = SortQuery.sortByFields(searchTaskRequest);

        Pageable page = PageRequest.of(firstResult, maxResults, sort);
        final Page<TaskResource> all = taskResourceRepository.findAll(taskResourceSpecification, page);
        return all.toList();
    }

}
