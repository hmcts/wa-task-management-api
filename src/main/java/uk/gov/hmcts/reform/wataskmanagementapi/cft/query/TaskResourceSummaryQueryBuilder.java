package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResourceSummary;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Selection;

public class TaskResourceSummaryQueryBuilder extends TaskResourceQueryBuilder<TaskResourceSummary> {

    public TaskResourceSummaryQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public CriteriaQuery<TaskResourceSummary> createQuery() {
        return builder.createQuery(TaskResourceSummary.class);
    }

    @Override
    public TypedQuery<TaskResourceSummary> build() {
        List<Selection<?>> selections = List.of(root.get("taskId"),
                                                root.get("dueDateTime"),
                                                root.get("caseId"),
                                                root.get("caseName"),
                                                root.get("caseCategory"),
                                                root.get("locationName"),
                                                root.get("title"),
                                                root.get("priorityDate"),
                                                root.get("majorPriority"),
                                                root.get("minorPriority"));

        CriteriaQuery<TaskResourceSummary> criteriaQuery =  query.multiselect(selections).distinct(true);

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
        if (orders != null && !orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
        return entityManager.createQuery(criteriaQuery);
    }
}
