package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

public class SelectTaskResourceQueryBuilder extends TaskResourceQueryBuilder<TaskResource> {

    private boolean shouldFetchTaskRoles;

    public SelectTaskResourceQueryBuilder(EntityManager entityManager, boolean shouldFetchTaskRoles) {
        super(entityManager);
        this.shouldFetchTaskRoles = shouldFetchTaskRoles;
    }

    public SelectTaskResourceQueryBuilder(EntityManager entityManager) {
        this(entityManager, false);
    }

    @Override
    public CriteriaQuery<TaskResource> createQuery() {
        return builder.createQuery(TaskResource.class);
    }

    @Override
    public TypedQuery<TaskResource> build() {
        if (shouldFetchTaskRoles) {
            root.fetch("taskRoleResources");
        }
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
