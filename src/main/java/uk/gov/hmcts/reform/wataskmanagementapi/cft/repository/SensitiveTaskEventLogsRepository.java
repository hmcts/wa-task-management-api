package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;

import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

public interface SensitiveTaskEventLogsRepository extends CrudRepository<SensitiveTaskEventLog, String>,
    JpaSpecificationExecutor<SensitiveTaskEventLog> {

    String CLEANUP_SENSITIVE_LOG_ENTRIES = """
        delete from
        cft_task_db.sensitive_task_event_logs t
        where
        t.expiry_time < :timestamp
        """;

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    @Transactional
    @NonNull
    @Override
    Optional<SensitiveTaskEventLog> findById(@NonNull String id);

    @Modifying
    @Query(value = CLEANUP_SENSITIVE_LOG_ENTRIES, nativeQuery = true)
    int cleanUpSensitiveLogs(@Param("timestamp") LocalDateTime timestamp);

}
