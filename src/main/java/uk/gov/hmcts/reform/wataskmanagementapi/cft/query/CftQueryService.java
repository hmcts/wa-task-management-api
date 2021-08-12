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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

        final List<SortingParameter> sortingParameters = searchTaskRequest.getSortingParameters();

        Sort sort;
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            sort = Sort.by("dueDateTime").descending();
        } else {
            final List<Sort> sortList = generateSortList(sortingParameters);
            sort = andSort(sortList).orElse(Sort.unsorted());
        }

        Pageable page = PageRequest.of(firstResult, maxResults, sort);
        final Page<TaskResource> all = taskResourceRepository.findAll(taskResourceSpecification, page);
        return all.toList();
    }

    private List<Sort> generateSortList(List<SortingParameter> sortingParameters) {
        return sortingParameters.stream().map((sortingParameter) -> {
            switch (sortingParameter.getSortOrder()) {
                case ASCENDANT:
                    return Sort.by(Sort.Order.asc(sortingParameter.getSortBy().getCamundaVariableName()));
                case DESCENDANT:
                    return Sort.by(Sort.Order.desc(sortingParameter.getSortBy().getCamundaVariableName()));
                default:
                    return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private <V extends Sort> Optional<Sort> andSort(List<V> criteria) {

        Iterator<V> itr = criteria.iterator();
        if (itr.hasNext()) {
            Sort sort = itr.next();
            while (itr.hasNext()) {
                sort = sort.and(itr.next());
            }
            return Optional.of(sort);
        }
        return Optional.empty();
    }
}
