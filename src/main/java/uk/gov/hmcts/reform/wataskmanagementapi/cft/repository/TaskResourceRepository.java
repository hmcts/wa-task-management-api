package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

@Repository
public interface TaskResourceRepository extends CrudRepository<TaskResource, String> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    Optional<TaskResource> findById(String id);

    Optional<TaskResource> getByTaskId(String id);

    TaskResource saveAndFlush(TaskResource taskResource);
}
