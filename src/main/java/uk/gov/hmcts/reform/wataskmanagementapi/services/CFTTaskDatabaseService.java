package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.SearchFilterSignatureBuilder;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.TooManyMethods"
})
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

    public Optional<TaskResource> findByIdAndWaitAndObtainPessimisticWriteLock(String taskId) {
        return tasksRepository.findByIdAndWaitForLock(taskId);
    }

    public Optional<TaskResource> findByIdOnly(String taskId) {
        return tasksRepository.getByTaskId(taskId);
    }

    public List<TaskResource> findByCaseIdOnly(String caseId) {
        return tasksRepository.getByCaseId(caseId);
    }

    public List<TaskResourceCaseQueryBuilder> findByTaskIdsByCaseId(final String caseId) {
        return tasksRepository.getTaskIdsByCaseId(caseId);
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

    public void deleteTasks(final List<String> taskIds) {
        tasksRepository.deleteAllById(taskIds);
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

    public GetTasksResponse<Task> searchForTasks(int firstResult,
                                                 int maxResults,
                                                 SearchRequest searchRequest,
                                                 AccessControlResponse accessControlResponse) {

        List<RoleAssignment> roleAssignments = new ArrayList<>(accessControlResponse.getRoleAssignments());
        Set<String> filterSignature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);
        Set<String> roleSignature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);
        List<String> excludeCaseIds = buildExcludedCaseIds(roleAssignments);

        log.debug("Task search for filter signatures {} and role signatures {}", filterSignature, roleSignature);
        List<String> taskIds = tasksRepository.searchTasksIds(firstResult, maxResults, filterSignature, roleSignature,
            excludeCaseIds, searchRequest);

        if (isEmpty(taskIds)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCount(filterSignature, roleSignature, excludeCaseIds, searchRequest);

        Sort sort = TaskSearchSortProvider.getSortOrders(searchRequest);
        final List<TaskResource> taskResources = tasksRepository.findAllByTaskIdIn(taskIds, sort);

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                    taskResource,
                    roleAssignments,
                    true
                )
            )
            .collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, count);
    }

    public List<TaskResource> findTaskToUpdateIndex() {
        return tasksRepository.findByIndexedFalse();
    }

    private List<String> buildExcludedCaseIds(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
            .map(ra -> ra.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
