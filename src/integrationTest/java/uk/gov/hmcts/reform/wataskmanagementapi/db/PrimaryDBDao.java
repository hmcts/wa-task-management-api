package uk.gov.hmcts.reform.wataskmanagementapi.db;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Slf4j
public class PrimaryDBDao {

    private final String jdbcUrl;
    private final String userName;
    private final String password;

    public PrimaryDBDao(String jdbcUrl, String userName, String password) {
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.password = password;
    }

    public void callPrimaryCleanupFunction(OffsetDateTime createdTimeFrom, OffsetDateTime createdTimeTo) {

        log.info("From time: " + Timestamp.valueOf(createdTimeFrom.toLocalDateTime()));
        log.info("To time: " + Timestamp.valueOf(createdTimeTo.toLocalDateTime()));
        String runFunction = " select cft_task_db.task_cleanup_between_dates_primary( ?,? ) ";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             PreparedStatement preparedStatement = conn.prepareStatement(runFunction)) {

            preparedStatement.setTimestamp(1, Timestamp.valueOf(createdTimeFrom.toLocalDateTime()));
            preparedStatement.setTimestamp(2, Timestamp.valueOf(createdTimeTo.toLocalDateTime()));

            preparedStatement.execute();

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
