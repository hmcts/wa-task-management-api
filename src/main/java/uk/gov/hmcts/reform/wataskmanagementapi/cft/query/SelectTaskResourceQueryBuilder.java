package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;

public class SelectTaskResourceQueryBuilder extends TaskResourceQueryBuilder<TaskResource> {

    public SelectTaskResourceQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public CriteriaQuery<TaskResource> createQuery() {
        return builder.createQuery(TaskResource.class);
    }

    @Override
    public TypedQuery<TaskResource> build() {
        root.fetch("taskRoleResources", JoinType.INNER);
        CriteriaQuery<TaskResource> criteriaQuery = query.select(root).distinct(true);

        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
        if (orders != null && !orders.isEmpty()) {
            criteriaQuery.orderBy(orders);
        }

        return entityManager.createQuery(criteriaQuery);
    }
}
