package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

public class SelectTaskResourceQueryBuilder extends TaskResourceQueryBuilder<TaskResource> {

    private boolean fetchTaskRole;

    public SelectTaskResourceQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    public void fetchTaskRole(boolean fetchTaskRole) {
        this.fetchTaskRole = fetchTaskRole;
    }

    @Override
    public CriteriaQuery<TaskResource> createQuery() {
        return builder.createQuery(TaskResource.class);
    }

    @Override
    public TypedQuery<TaskResource> build() {
        if (fetchTaskRole) {
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
