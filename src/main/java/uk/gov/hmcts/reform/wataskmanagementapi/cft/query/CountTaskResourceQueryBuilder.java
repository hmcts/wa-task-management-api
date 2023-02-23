package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

public class CountTaskResourceQueryBuilder extends TaskResourceQueryBuilder<Long> {
    public Subquery<TaskResource> subQuery;
    public Root<TaskResource> subRoot;

    public CountTaskResourceQueryBuilder(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public CriteriaQuery<Long> createQuery() {
        return builder.createQuery(Long.class);
    }

    public CountTaskResourceQueryBuilder createSubQuery() {
        this.subQuery = query.subquery(TaskResource.class);
        return this;
    }

    public CountTaskResourceQueryBuilder createSubRoot() {
        this.subRoot = subQuery.from(TaskResource.class);
        return this;
    }

    @Override
    public TypedQuery<Long> build() {
        Expression<Long> select = builder.count(root);
        CriteriaQuery<Long> criteriaQuery = query.select(select);

        subQuery.select(subRoot.get("taskId")).distinct(true);
        subQuery.where(predicate);
        CriteriaBuilder.In<TaskResource> value = builder.in(root).value(subQuery);
        criteriaQuery.where(value);
        return entityManager.createQuery(criteriaQuery);
    }
}
