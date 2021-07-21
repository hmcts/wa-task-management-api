package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import java.util.Optional;
import javax.persistence.LockModeType;

public interface TaskResourceRepository extends CrudRepository<TaskResource, String> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TaskResource> findById(String id);

    TaskResource saveAndFlush(TaskResource taskResource);
}
