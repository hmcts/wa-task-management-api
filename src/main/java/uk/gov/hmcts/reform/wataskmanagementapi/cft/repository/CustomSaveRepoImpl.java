package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class CustomSaveRepoImpl implements CustomSaveRepo<TaskResource> {

    @PersistenceContext
    private final EntityManager entityManager;

    public CustomSaveRepoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public <S extends TaskResource> S insert(S entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
