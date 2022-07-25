package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder.ASCENDANT;

@Slf4j
@Service
@SuppressWarnings({"PMD.UnnecessaryFullyQualifiedName"})
public class TaskResourceDao {

    @PersistenceContext
    private final EntityManager entityManager;

    public TaskResourceDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Object[]> getTaskResourceSummary(int firstResult,
                                                            int maxResults,
                                                            SearchTaskRequest searchTaskRequest,
                                                            List<RoleAssignment> roleAssignments,
                                                            List<PermissionTypes> permissionsRequired) {
        Pageable page = OffsetPageableRequest.of(firstResult, maxResults);
        TaskResourceSummaryQueryBuilder summaryQueryBuilder = new TaskResourceSummaryQueryBuilder(entityManager);
        CriteriaBuilder builder = summaryQueryBuilder.builder;
        Root<TaskResource> root = summaryQueryBuilder.root;

        List<Selection<?>> selections = getSelections(searchTaskRequest, root);

        Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            builder,
            root
        );

        List<Order> orders = getSortOrders(searchTaskRequest, builder, root);
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
            selections.addAll(List.of(root.get("priorityDate"), root.get("majorPriority"), root.get("minorPriority")));
        } else {
            sortingParameters.forEach(p -> selections.add(root.get(p.getSortBy().getCftVariableName())));
        }
        return selections;
    }

    public List<TaskResource> getTaskResources(SearchTaskRequest searchTaskRequest,
                                               List<Object[]> taskResourcesSummary) {
        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager, true);
        CriteriaBuilder builder = selectQueryBuilder.builder;
        Root<TaskResource> root = selectQueryBuilder.root;

        List<String> taskIds = taskResourcesSummary.stream()
            .map(o -> stream(o).findFirst())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Object::toString)
            .collect(Collectors.toList());
        List<Order> orders = getSortOrders(searchTaskRequest, builder, root);
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
        return selectQueryBuilder
            .where(selectPredicate)
            .build()
            .getResultList().stream()
            .findFirst();
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

    private List<Order> getSortOrders(SearchTaskRequest searchTaskRequest,
                                      CriteriaBuilder builder,
                                      Root<TaskResource> root) {
        final List<SortingParameter> sortingParameters = searchTaskRequest.getSortingParameters();

        List<Order> orders = new ArrayList<>();
        if (sortingParameters == null || sortingParameters.isEmpty()) {
            Stream.of("majorPriority", "priorityDate", "minorPriority")
                .map(s -> builder.asc(root.get(s)))
                .collect(Collectors.toCollection(() -> orders));
        } else {
            orders.addAll(generateOrders(sortingParameters, builder, root));
        }

        return orders;
    }

    private List<Order> generateOrders(List<SortingParameter> sortingParameters,
                                       CriteriaBuilder criteriaBuilder,
                                       Root<TaskResource> root) {
        return sortingParameters.stream()
            .filter(s -> s.getSortOrder() != null)
            .map(sortingParameter -> {
                if (sortingParameter.getSortOrder() == ASCENDANT) {
                    return criteriaBuilder.asc(root.get(sortingParameter.getSortBy().getCftVariableName()));
                } else {
                    return criteriaBuilder.desc(root.get(sortingParameter.getSortBy().getCftVariableName()));
                }
            })
            .filter(Objects::nonNull).collect(Collectors.toList());
    }
}
