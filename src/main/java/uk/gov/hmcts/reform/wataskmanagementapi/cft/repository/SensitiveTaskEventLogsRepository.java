package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;

import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

public interface SensitiveTaskEventLogsRepository extends CrudRepository<SensitiveTaskEventLog, String>,
    JpaSpecificationExecutor<SensitiveTaskEventLog> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
        Optional<SensitiveTaskEventLog> findById(@NonNull String id);
}
