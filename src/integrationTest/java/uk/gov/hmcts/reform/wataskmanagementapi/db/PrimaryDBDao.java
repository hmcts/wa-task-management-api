package uk.gov.hmcts.reform.wataskmanagementapi.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import javax.sql.DataSource;

@Slf4j
public class PrimaryDBDao {

    private final JdbcTemplate jdbcTemplate;

    private final String jdbcUrl;
    private final String userName;
    private final String password;

    public PrimaryDBDao(String jdbcUrl, String userName, String password) {
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.password = password;
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

    public void insertPrimaryCleanupFunction(String insertFunction) {

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             PreparedStatement preparedStatement = conn.prepareStatement(insertFunction)) {
            preparedStatement.execute();

            log.info("Primary insert function inserted. ");

        } catch (SQLException e) {
            log.error("Procedure call callMarkReportTasksForRefresh failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
            throw new ReplicationException("An error occurred while executing mark_report_tasks_for_refresh", e);
        } catch (Exception e) {
            log.error("Procedure call callMarkReportTasksForRefresh failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
            throw new ReplicationException("An error occurred while executing mark_report_tasks_for_refresh", e);
        }
    }

}
