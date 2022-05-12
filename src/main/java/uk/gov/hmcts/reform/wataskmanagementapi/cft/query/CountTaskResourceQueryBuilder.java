package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;

public class CountTaskResourceQueryBuilder extends TaskResourceQueryBuilder<Long> {

    public CountTaskResourceQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public CriteriaQuery<Long> createQuery() {
        return builder.createQuery(Long.class);
    }

    @Override
    public TypedQuery<Long> build() {
        Expression<Long> select = builder.countDistinct(root);

        CriteriaQuery<Long> criteriaQuery = query.select(select).distinct(true);
        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
        if (orders != null && !orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }
        return entityManager.createQuery(criteriaQuery);
    }
}
