package uk.gov.hmcts.reform.wataskmanagementapi.db;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class MIReplicaDBDao {

    private final String jdbcUrl;
    private final String userName;
    private final String password;

    public MIReplicaDBDao(String jdbcUrl, String userName, String password) {
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.password = password;
    }


    public void callMarkReportTasksForRefresh(List<String> caseIdList, List<String> taskIdList, String jurisdiction,
                                              String caseTypeId, List<String> stateList,
                                              OffsetDateTime createdBefore) {

        log.info(String.valueOf(Timestamp.valueOf(createdBefore.toLocalDateTime())));
        String runFunction = " call cft_task_db.mark_report_tasks_for_refresh( ?,?,?,?,?,? ) ";

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

    public List<Timestamp> callGetReplicaTaskRequestRefreshTimes(List<String> taskIdList) {

        String runFunction = "{ ? = call cft_task_db.get_report_refresh_request_times( ? ) }";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             CallableStatement callableStatement = conn.prepareCall(runFunction)) {

            Array taskIds = conn.createArrayOf("TEXT", taskIdList.toArray());

            callableStatement.registerOutParameter(1, Types.ARRAY);
            callableStatement.setArray(2, taskIds);

            callableStatement.execute();
            Array reportRefreshRequestTimes = callableStatement.getArray(1);
            log.info(reportRefreshRequestTimes.toString());
            Timestamp[] stringReportRequestTimes = (Timestamp[]) reportRefreshRequestTimes.getArray();
            return Arrays.stream(stringReportRequestTimes).filter(Objects::nonNull).toList();

        } catch (SQLException e) {
            log.error("Procedure call callGetReplicaTaskRequestRefreshTimes failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
            throw new ReplicationException("An error occurred while executing get_report_refresh_request_times", e);
        } catch (Exception e) {
            log.error("Procedure call callGetReplicaTaskRequestRefreshTimes failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
            throw new ReplicationException("An error occurred while executing get_report_refresh_request_times", e);
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
}
