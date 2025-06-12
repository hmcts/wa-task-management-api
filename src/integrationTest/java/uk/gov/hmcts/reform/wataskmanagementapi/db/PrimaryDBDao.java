package uk.gov.hmcts.reform.wataskmanagementapi.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import javax.sql.DataSource;

@Slf4j
public class PrimaryDBDao {

    private final JdbcTemplate jdbcTemplate;

    public PrimaryDBDao(String jdbcUrl, String userName, String password) {
        DataSource dataSource = new DriverManagerDataSource(jdbcUrl, userName, password);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void callPrimaryCleanupFunction(OffsetDateTime createdTimeFrom, OffsetDateTime createdTimeTo) {
        log.info("From time: {}", createdTimeFrom);
        log.info("To time: {}", createdTimeTo);

        String runFunction = "select cft_task_db.task_cleanup_between_dates_primary(?, ?)";

        try {

            jdbcTemplate.execute(runFunction, (PreparedStatement ps) -> {
                ps.setTimestamp(1, Timestamp.valueOf(createdTimeFrom.toLocalDateTime()));
                ps.setTimestamp(2, Timestamp.valueOf(createdTimeTo.toLocalDateTime()));
                ps.execute();
                return null;
            });

        } catch (Exception e) {
            log.error("Procedure call failed: {}", e.getMessage(), e);
            throw new ReplicationException("An error occurred while executing task_cleanup_between_dates_primary", e);
        }
    }

}
