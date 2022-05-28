package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResourceSummary;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

@Slf4j
@Service
@SuppressWarnings({"PMD.UnnecessaryFullyQualifiedName"})
public class TaskResourceDao {

    @PersistenceContext
    private final EntityManager entityManager;

    public TaskResourceDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<TaskResourceSummary> getTaskResourceSummary(int firstResult,
                                                            int maxResults,
                                                            SearchTaskRequest searchTaskRequest,
                                                            List<RoleAssignment> roleAssignments,
                                                            List<PermissionTypes> permissionsRequired) {
        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        Pageable page = OffsetPageableRequest.of(firstResult, maxResults, sort);
        TaskResourceSummaryQueryBuilder summaryQueryBuilder = new TaskResourceSummaryQueryBuilder(entityManager);
        CriteriaBuilder builder = summaryQueryBuilder.builder;
        Root<TaskResource> root = summaryQueryBuilder.root;

        List<Selection<?>> selections = getSelections(searchTaskRequest, root);

        List<Order> orders = SortQuery.sortByFields(searchTaskRequest, builder, root);
        Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            builder,
            root
        );

        return summaryQueryBuilder
            .where(selectPredicate)
            .withOrders(orders)
            .withSelections(selections)
            .build()
            .setFirstResult((int) page.getOffset())
            .setMaxResults(page.getPageSize())
            .getResultList();

    }

    private List<Selection<?>> getSelections(SearchTaskRequest searchTaskRequest, Root<TaskResource> root) {
        List<Selection<?>> selections = new ArrayList<>();

        selections.add(root.get("taskId"));

        List<SortingParameter> sortingParameters = searchTaskRequest.getSortingParameters();
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            selections.add(root.get("dueDateTime"));
        } else {
            sortingParameters.forEach(p -> selections.add(root.get(p.getSortBy().getCftVariableName())));
        }
        return selections;
    }

    public List<TaskResource> getTaskResources(SearchTaskRequest searchTaskRequest,
                                               List<TaskResourceSummary> taskResourcesSummary) {
        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);
        CriteriaBuilder builder = selectQueryBuilder.builder;
        Root<TaskResource> root = selectQueryBuilder.root;

        List<String> taskIds = taskResourcesSummary.stream()
            .map(TaskResourceSummary::getTaskId)
            .collect(Collectors.toList());
        List<Order> orders = SortQuery.sortByFields(searchTaskRequest, builder, root);
        Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskQuery(taskIds, builder, root);

        return selectQueryBuilder
            .where(selectPredicate)
            .withOrders(orders)
            .build()
            .getResultList();
    }

    public Long getTotalCount(SearchTaskRequest searchTaskRequest,
                              List<RoleAssignment> roleAssignments,
                              List<PermissionTypes> permissionsRequired) {

        CountTaskResourceQueryBuilder countQueryBuilder = new CountTaskResourceQueryBuilder(entityManager)
            .createSubQuery()
            .createSubRoot();

        Predicate countPredicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            countQueryBuilder.builder,
            countQueryBuilder.subRoot
        );

        return countQueryBuilder
            .where(countPredicate)
            .build()
            .getSingleResult();
    }

    public Optional<TaskResource> getTask(String taskId,
                                          List<RoleAssignment> roleAssignments,
                                          List<PermissionTypes> permissionsRequired
    ) {
        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);

        final Predicate selectPredicate = TaskSearchQueryBuilder.buildSingleTaskQuery(
            taskId,
            roleAssignments,
            permissionsRequired,
            selectQueryBuilder.builder,
            selectQueryBuilder.root
        );

        selectQueryBuilder.where(selectPredicate).build().getResultList();

        try {
            return selectQueryBuilder
                .where(selectPredicate)
                .build()
                .getResultList().stream()
                .findFirst();
        } catch (NoResultException ne) {
            return Optional.empty();
        }
    }

    public List<TaskResource> getCompletableTaskResources(SearchEventAndCase searchEventAndCase,
                                                          List<RoleAssignment> roleAssignments,
                                                          List<PermissionTypes> permissionsRequired,
                                                          List<String> taskTypes) {
        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);

        final Predicate selectPredicate = TaskSearchQueryBuilder.buildQueryForCompletable(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            taskTypes,
            selectQueryBuilder.builder,
            selectQueryBuilder.root
        );

        return selectQueryBuilder
            .where(selectPredicate)
            .build()
            .getResultList();
    }
}
