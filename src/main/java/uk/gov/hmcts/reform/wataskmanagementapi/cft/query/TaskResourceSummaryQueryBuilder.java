package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResourceSummary;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

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

        CriteriaQuery<TaskResourceSummary> criteriaQuery = query.multiselect(selections).distinct(true);

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
        if (orders != null && !orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
        return entityManager.createQuery(criteriaQuery);
    }
}
