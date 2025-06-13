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
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;


@Slf4j
public class MIReplicaDBDao {

    private final String jdbcUrl;
    private final String userName;
    private final String password;
    private final JdbcTemplate jdbcTemplate;

    public MIReplicaDBDao(String jdbcUrl, String userName, String password) {
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.password = password;

        DataSource dataSource = new DriverManagerDataSource(jdbcUrl, userName, password);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public void callMarkReportTasksForRefresh(List<String> caseIdList, List<String> taskIdList, String jurisdiction,
                                              String caseTypeId, List<String> stateList,
                                              OffsetDateTime createdBefore, OffsetDateTime createdAfter) {

        log.info(String.valueOf(Timestamp.valueOf(createdBefore.toLocalDateTime())));
        String runFunction = " call cft_task_db.mark_report_tasks_for_refresh( ?,?,?,?,?,?,? ) ";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             PreparedStatement preparedStatement = conn.prepareStatement(runFunction)) {

            preparedStatement.setArray(1, conn.createArrayOf(
                "TEXT",
                Objects.isNull(taskIdList) ? null : taskIdList.toArray()
            ));
            preparedStatement.setArray(2, conn.createArrayOf(
                "TEXT",
                Objects.isNull(caseIdList) ? null : caseIdList.toArray()
            ));
            preparedStatement.setString(3, jurisdiction);
            preparedStatement.setString(4, caseTypeId);
            preparedStatement.setArray(5, conn.createArrayOf(
                "TEXT",
                Objects.isNull(stateList) ? null : stateList.toArray()
            ));
            preparedStatement.setTimestamp(6, Timestamp.valueOf(createdBefore.toLocalDateTime()));
            preparedStatement.setTimestamp(7, Timestamp.valueOf(createdAfter.toLocalDateTime()));

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

    public void callRefreshReportTasks(int refreshRecordsCount) {
        log.info(String.format("callRefreshReportTasks with maxRecordsCount : %s", refreshRecordsCount));

        String runFunction = " call cft_task_db.refresh_report_tasks( ? ) ";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             PreparedStatement preparedStatement = conn.prepareStatement(runFunction)) {

            preparedStatement.setInt(1, refreshRecordsCount);
            preparedStatement.execute();

        } catch (SQLException e) {
            log.error("Procedure call refresh_report_tasks failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
            throw new ReplicationException("An error occurred while executing refresh_report_tasks", e);
        } catch (Exception e) {
            log.error("Procedure call refresh_report_tasks failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
            throw new ReplicationException("An error occurred while executing refresh_report_tasks", e);
        }

    }

    public void callReplicaCleanupFunction(OffsetDateTime createdTimeFrom, OffsetDateTime createdTimeTo) {

        log.info("From time: {}", Timestamp.valueOf(createdTimeFrom.toLocalDateTime()));
        log.info("To time: {}", Timestamp.valueOf(createdTimeTo.toLocalDateTime()));

        String runFunction = "select cft_task_db.task_cleanup_between_dates_replica(?, ?)";

        try {

            jdbcTemplate.execute(runFunction, (PreparedStatement ps) -> {
                ps.setTimestamp(1, Timestamp.valueOf(createdTimeFrom.toLocalDateTime()));
                ps.setTimestamp(2, Timestamp.valueOf(createdTimeTo.toLocalDateTime()));
                ps.execute();
                return null;
            });

        } catch (Exception e) {
            log.error("Procedure call callReplicaCleanupFunction failed: {}", e.getMessage(), e);
            throw new ReplicationException("An error occurred while executing task_cleanup_between_dates_replica", e);
        }
    }

}
