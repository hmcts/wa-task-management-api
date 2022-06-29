package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

public class TaskResourceSummaryQueryBuilder extends TaskResourceQueryBuilder<Object[]> {

    public TaskResourceSummaryQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public CriteriaQuery<Object[]> createQuery() {
        return builder.createQuery(Object[].class);
    }

    @Override
    public TypedQuery<Object[]> build() {

        CriteriaQuery<Object[]> criteriaQuery = query.multiselect(selections).distinct(true);

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
        if (orders != null && !orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
        return entityManager.createQuery(criteriaQuery);
    }
}
