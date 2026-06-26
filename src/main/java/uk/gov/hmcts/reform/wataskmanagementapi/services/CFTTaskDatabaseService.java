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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.TooManyMethods"
})
public class CFTTaskDatabaseService {

    private static final int ROLE_ASSIGNMENTS_LOG_THRESHOLD = 100;
    private static final String OWN_AND_CLAIM_PERMISSION = "a";
    private static final String MANAGE_PERMISSION = "m";
    private static final String READ_PERMISSION = "r";

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

    public Optional<TaskResource> findByIdAndStateInObtainPessimisticWriteLock(String taskId,
                                                                               List<CFTTaskState> states) {
        return tasksRepository.findByIdAndStateIn(taskId, states);
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

    public List<String> getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
        List<CFTTaskState> states, OffsetDateTime reconfigureRequestTime) {
        return tasksRepository.findTaskIdsByStateInAndReconfigureRequestTimeGreaterThan(
            states, reconfigureRequestTime);
    }

    public List<TaskResource> getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
        List<String> taskIds, List<CFTTaskState> states, OffsetDateTime retryWindow) {
        return tasksRepository.findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
            taskIds, states, retryWindow);
    }

    public List<TaskResource> getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(
        List<CFTTaskState> states, OffsetDateTime retryWindow) {
        return tasksRepository.findByStateInAndReconfigureRequestTimeIsLessThan(states, retryWindow);
    }

    public TaskResource saveTask(TaskResource task) {
        if (task.getPriorityDate() == null) {
            task.setPriorityDate(task.getDueDateTime());
        }
        return tasksRepository.save(task);
    }

    public void markTasksToDeleteByTaskId(final List<String> taskIds) {
        tasksRepository.updateTaskDeletionTimestampByTaskIds(taskIds);
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

        List<RoleAssignment> roleAssignments = accessControlResponse.getRoleAssignments();

        if (ROLE_ASSIGNMENTS_LOG_THRESHOLD <= roleAssignments.size()) {
            log.info("Total volume of Role Assignments for current user: {}", roleAssignments.size());
        }

        List<String> excludeCaseIds = buildExcludedCaseIds(roleAssignments);
        List<TaskSearchRoleCriteria> roleCriteria = buildRoleCriteria(roleAssignments, searchRequest);

        log.info("Task search excluded case ids {}", excludeCaseIds);
        List<String> taskIds = tasksRepository.searchTasksIds(
            firstResult, maxResults, roleCriteria, excludeCaseIds, searchRequest
        );

        if (isEmpty(taskIds)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCount(roleCriteria, excludeCaseIds, searchRequest);

        Sort sort = TaskSearchSortProvider.getSortOrders(searchRequest);
        final List<TaskResource> taskResources = tasksRepository.findAllByTaskIdIn(taskIds, sort);

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                     cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                         taskResource,
                         roleAssignments
                     )
            ).toList();

        return new GetTasksResponse<>(tasks, count);
    }

    public List<TaskResource> findTaskToUpdateIndex() {
        return tasksRepository.findByIndexedFalseAndStateIn(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));
    }

    public List<TaskResource> findLastFiveUpdatedTasks() {
        return tasksRepository.findTop5ByOrderByLastUpdatedTimestampDesc();
    }

    private List<String> buildExcludedCaseIds(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
            .map(ra -> ra.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()))
            .filter(Objects::nonNull)
            .toList();
    }

    private List<TaskSearchRoleCriteria> buildRoleCriteria(List<RoleAssignment> roleAssignments,
                                                           SearchRequest searchRequest) {
        List<TaskSearchRoleCriteria> roleCriteria = new ArrayList<>();

        for (RoleAssignment roleAssignment : roleAssignments) {
            if (!canMatchSearch(roleAssignment, searchRequest)) {
                continue;
            }

            for (String authorizationValue : authorizations(roleAssignment, searchRequest)) {
                roleCriteria.add(new TaskSearchRoleCriteria(
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.JURISDICTION).orElse(null),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.REGION).orElse(null),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.BASE_LOCATION).orElse(null),
                    roleAssignment.getRoleName(),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).orElse(null),
                    permissionRequirement(searchRequest),
                    roleAssignment.getClassification().getAbbreviation(),
                    authorizationValue
                ));
            }
        }

        return roleCriteria;
    }

    private boolean canMatchSearch(RoleAssignment roleAssignment, SearchRequest searchRequest) {
        return roleAssignment.getRoleName() != null
               && roleAssignment.getClassification() != null
               && roleAssignment.getClassification().getAbbreviation() != null
               && List.of(GrantType.STANDARD, GrantType.SPECIFIC, GrantType.CHALLENGED).contains(
                   roleAssignment.getGrantType())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.JURISDICTION,
                                       searchRequest.getJurisdictions())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.REGION, searchRequest.getRegions())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.BASE_LOCATION,
                                       searchRequest.getLocations())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.CASE_ID, searchRequest.getCaseIds());
    }

    private boolean matchesRoleAttribute(RoleAssignment roleAssignment,
                                         RoleAttributeDefinition attribute,
                                         List<String> requestedValues) {
        return isEmpty(requestedValues)
               || roleAssignment.getAttributeValue(attribute).isEmpty()
               || requestedValues.contains(roleAssignment.getAttributeValue(attribute).get());
    }

    private List<String> authorizations(RoleAssignment roleAssignment, SearchRequest searchRequest) {
        List<String> authorizationValues = new ArrayList<>();
        authorizationValues.add(null);

        if (searchRequest.isAvailableTasksOnly()
            && roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).isEmpty()
            && !isEmpty(roleAssignment.getAuthorisations())) {
            authorizationValues.addAll(roleAssignment.getAuthorisations());
        }

        return authorizationValues.stream().distinct().toList();
    }

    private String permissionRequirement(SearchRequest searchRequest) {
        if (searchRequest.isAvailableTasksOnly()) {
            return OWN_AND_CLAIM_PERMISSION;
        } else if (searchRequest.isAllWork()) {
            return MANAGE_PERMISSION;
        }
        return READ_PERMISSION;
    }
}
