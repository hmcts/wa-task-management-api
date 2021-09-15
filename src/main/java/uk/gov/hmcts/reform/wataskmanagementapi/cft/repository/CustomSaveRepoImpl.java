package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.QueryHint;

import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;

@Slf4j
public class CustomSaveRepoImpl implements CustomSaveRepo<TaskResource> {

    @PersistenceContext
    private final EntityManager entityManager;

    public CustomSaveRepoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Lock(PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
    public <S extends TaskResource> S insert(S entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Override
    @Transactional
    public <S extends TaskResource> void insertWithQuery(S entity) {
        entityManager.createNativeQuery("INSERT INTO cft_task_db.tasks (task_id, assignee) VALUES (?,?)")
            .setParameter(1, entity.getTaskId())
            .setParameter(2, entity.getAssignee())
            .executeUpdate();
    }
}
