package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public abstract class TaskResourceQueryBuilder<T> {
    public final EntityManager entityManager;
    public final CriteriaBuilder builder;
    public final CriteriaQuery<T> query;
    public final Root<TaskResource> root;
    public Predicate predicate;
    public List<Order> orders;

    public TaskResourceQueryBuilder(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.builder = entityManager.getCriteriaBuilder();
        this.query = createQuery();
        this.root = query.from(TaskResource.class);
    }

    protected abstract CriteriaQuery<T> createQuery();

    public abstract TypedQuery<T> build();

    public TaskResourceQueryBuilder<T> where(Predicate predicate) {
        this.predicate = predicate;
        return this;
    }

    public TaskResourceQueryBuilder<T> withOrders(List<Order> orders) {
        this.orders = orders;
        return this;
    }
}
