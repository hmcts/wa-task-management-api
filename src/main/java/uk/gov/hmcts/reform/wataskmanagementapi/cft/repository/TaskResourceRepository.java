package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

public interface TaskResourceRepository extends CrudRepository<TaskResource, String>, CustomSaveRepo<TaskResource> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
    Optional<TaskResource> findById(@NonNull String id);

    Optional<TaskResource> getByTaskId(String id);

    <S extends TaskResource> S saveAndFlush(S entity);

    @Query(value = "SELECT * FROM cft_task_db.tasks WHERE task_id = ?1", nativeQuery = true)
    TaskResource selectTask(String id);



}
