package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;

import java.util.Optional;

public interface SensitiveTaskEventLogsRepository extends CrudRepository<SensitiveTaskEventLog, String>,
    JpaSpecificationExecutor<SensitiveTaskEventLog> {

    Optional<SensitiveTaskEventLog> getByTaskId(String taskId);
}
