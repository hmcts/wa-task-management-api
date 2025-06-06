package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.List;

public abstract class TaskResourceQueryBuilder<T> {
    public final EntityManager entityManager;
    public final CriteriaBuilder builder;
    public final CriteriaQuery<T> query;
    public final Root<TaskResource> root;
    public Predicate predicate;
    public List<Order> orders;
    List<Selection<?>> selections;

    protected TaskResourceQueryBuilder(EntityManager entityManager) {
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

    public TaskResourceQueryBuilder<T> withSelections(List<Selection<?>> selections) {
        this.selections = selections;
        return this;
    }
}
