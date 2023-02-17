package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder.ASCENDANT;

@Slf4j
@Service
public class CFTTaskDatabaseService {
    private final TaskResourceRepository tasksRepository;
    private final CFTTaskMapper cftTaskMapper;

    public CFTTaskDatabaseService(TaskResourceRepository tasksRepository,
                                  CFTTaskMapper cftTaskMapper) {
        this.tasksRepository = tasksRepository;
        this.cftTaskMapper = cftTaskMapper;
    }

    public Optional<TaskResource> findByIdAndObtainPessimisticWriteLock(String taskId) {
        return tasksRepository.findById(taskId);
    }

    public Optional<TaskResource> findByIdOnly(String taskId) {
        return tasksRepository.getByTaskId(taskId);
    }

    public List<TaskResource> findByCaseIdOnly(String caseId) {
        return tasksRepository.getByCaseId(caseId);
    }

    public List<TaskResource> getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
        List<String> caseIds, List<CFTTaskState> states) {
        return tasksRepository.findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(caseIds, states);
    }

    public List<TaskResource> getActiveTasksAndReconfigureRequestTimeGreaterThan(
        List<CFTTaskState> states, OffsetDateTime reconfigureRequestTime) {
        return tasksRepository.findByStateInAndReconfigureRequestTimeGreaterThan(
            states, reconfigureRequestTime);
    }

    public List<TaskResource> getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
        List<String> taskIds, List<CFTTaskState> states, OffsetDateTime retryWindow) {
        return tasksRepository.findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
            taskIds, states, retryWindow);
    }

    public TaskResource saveTask(TaskResource task) {
        if (task.getPriorityDate() == null) {
            task.setPriorityDate(task.getDueDateTime());
        }
        return tasksRepository.save(task);
    }

    public void insertAndLock(String taskId, OffsetDateTime dueDate) throws SQLException {
        OffsetDateTime created = OffsetDateTime.now();
        tasksRepository.insertAndLock(taskId, dueDate, created, dueDate);
    }

    public Optional<TaskResource> findTaskBySpecification(Specification<TaskResource> specification) {
        return tasksRepository.findOne(specification);
    }

    public Optional<String> findCaseId(String taskId) {
        Optional<TaskResource> taskResource = findByIdOnly(taskId);
        if (taskResource.isPresent() && taskResource.get().getCaseId() != null) {
            return Optional.of(taskResource.get().getCaseId());
        }
        return Optional.empty();
    }

    public GetTasksResponse<Task> searchForTasks(SearchRequest searchRequest,
                                                 AccessControlResponse accessControlResponse,
                                                 boolean granularPermissionResponseFeature) {

        Set<String> filters = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);

        String[] filterSignature = filters.toArray(new String[0]);

        List<String> taskIds = tasksRepository.searchTasksIds(filterSignature);

        if (isEmpty(taskIds)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        Sort sort = getOrders(searchRequest);

        final List<TaskResource> taskResources
            = tasksRepository.findAllByTaskIdIn(taskIds, sort);

        Long count = tasksRepository.searchTasksCount(filterSignature);
        List<RoleAssignment> roleAssignments = accessControlResponse.getRoleAssignments();

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                    taskResource,
                    roleAssignments,
                    granularPermissionResponseFeature
                )
            )
            .collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, count);
    }

    private Sort getOrders(SearchRequest searchRequest) {
        List<Sort.Order> orders = Stream.ofNullable(searchRequest.getSortingParameters())
            .flatMap(Collection::stream)
            .filter(s -> s.getSortOrder() != null)
            .map(sortingParameter -> {
                if (sortingParameter.getSortOrder() == ASCENDANT) {
                    return Sort.Order.asc(sortingParameter.getSortBy().getCftVariableName());
                } else {
                    return Sort.Order.desc(sortingParameter.getSortBy().getCftVariableName());
                }
            }).collect(Collectors.toList());

        Stream.of(MAJOR_PRIORITY, PRIORITY_DATE, MINOR_PRIORITY)
            .map(s -> Sort.Order.asc(s.value()))
            .collect(Collectors.toCollection(() -> orders));

        return Sort.by(orders);
    }
}
