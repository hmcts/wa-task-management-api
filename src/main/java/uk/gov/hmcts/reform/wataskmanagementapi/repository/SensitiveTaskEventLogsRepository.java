package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;

import java.time.LocalDateTime;

public interface SensitiveTaskEventLogsRepository extends CrudRepository<SensitiveTaskEventLog, String>,
    JpaSpecificationExecutor<SensitiveTaskEventLog> {

    String CLEANUP_SENSITIVE_LOG_ENTRIES = """
        delete from
        cft_task_db.sensitive_task_event_logs t
        where
        t.expiry_time < :timestamp
        """;

    @Modifying
    @Query(value = CLEANUP_SENSITIVE_LOG_ENTRIES, nativeQuery = true)
    int cleanUpSensitiveLogs(@Param("timestamp") LocalDateTime timestamp);

}
