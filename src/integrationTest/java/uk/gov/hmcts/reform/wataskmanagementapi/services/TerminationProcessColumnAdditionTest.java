package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.postgresql.util.PSQLException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("replica")
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TerminationProcessColumnAdditionTest extends ReplicaBaseTest {
    private static final String TERMINATION_PROCESS_COLUMN_NAME = "termination_process";
    private static final String TASKS_TABLE_NAME = "tasks";
    private static final String TASK_HISTORY_TABLE_NAME = "task_history";
    private static final String REPORTABLE_TASK_TABLE_NAME = "reportable_task";

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:scripts/wa/delete_task_data.sql",
            "classpath:scripts/wa/insert_task_data.sql"})
    void should_verify_termination_process_populated_for_a_task_in_primary_and_replica_tables() {
        String taskIdWithTerminationProcess = "9a6cc5cf-c973-11eb-bdba-0242ac111001";
        String taskIdWithOutTerminationProcess = "9a6cc5cf-c973-11eb-bdba-0242ac111011";
        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    String terminationProcess =
                        selectTaskColumnValueFromTable(taskIdWithTerminationProcess,
                                                       TASKS_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       container);
                    assertNotNull(terminationProcess);
                    String terminationProcessInReplicaDB =
                        selectTaskColumnValueFromTable(taskIdWithTerminationProcess,
                                                       TASKS_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       containerReplica);
                    assertEquals(terminationProcess, terminationProcessInReplicaDB);
                    terminationProcessInReplicaDB =
                        selectTaskColumnValueFromTable(taskIdWithTerminationProcess,
                                                       TASK_HISTORY_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       containerReplica);
                    assertEquals(terminationProcess, terminationProcessInReplicaDB);
                    terminationProcessInReplicaDB =
                        selectTaskColumnValueFromTable(taskIdWithTerminationProcess,
                                                       REPORTABLE_TASK_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       containerReplica);
                    assertEquals(terminationProcess, terminationProcessInReplicaDB);
                    terminationProcess =
                        selectTaskColumnValueFromTable(taskIdWithOutTerminationProcess,
                                                       TASKS_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       container);
                    assertNull(terminationProcess);
                    terminationProcessInReplicaDB =
                        selectTaskColumnValueFromTable(taskIdWithOutTerminationProcess,
                                                       TASKS_TABLE_NAME,
                                                       TERMINATION_PROCESS_COLUMN_NAME,
                                                       containerReplica);
                    assertNull(terminationProcessInReplicaDB);
                    return true;
                });
    }

    @Test
    void should_verify_if_termination_process_column_added_in_tables() {

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    boolean columnAdded = checkIfColumnAddedInTable(TASKS_TABLE_NAME,
                                                                    TERMINATION_PROCESS_COLUMN_NAME,
                                                                    container);
                    assertTrue(columnAdded);

                    columnAdded = checkIfColumnAddedInTable(TASKS_TABLE_NAME,


                    boolean columnAdded = checkIfColumnAddedInTable(TASKS_TABLE_NAME,
                                                            TERMINATION_PROCESS_COLUMN_NAME,
                                                            containerReplica);
                    assertTrue(columnAdded);
                    columnAdded = checkIfColumnAddedInTable(TASK_HISTORY_TABLE_NAME,
                                                            TERMINATION_PROCESS_COLUMN_NAME,
                                                            containerReplica);
                    assertTrue(columnAdded);
                    columnAdded = checkIfColumnAddedInTable(REPORTABLE_TASK_TABLE_NAME,
                                                            TERMINATION_PROCESS_COLUMN_NAME,
                                                            containerReplica);
                    assertTrue(columnAdded);
                    return true;
                });
    }

    private String selectTaskColumnValueFromTable(String taskId, String tableName, String columnName,
                                                  JdbcDatabaseContainer container) {
        String selectQuery = "select " + columnName + " from cft_task_db." + tableName + " where task_id = ? ";
        String columnValue = null;
        try (Connection conn = DriverManager.getConnection(container.getJdbcUrl(),
                                                            container.getUsername(),
                                                            container.getPassword());
             PreparedStatement preparedStatement = conn.prepareStatement(selectQuery)) {
            preparedStatement.setString(1, taskId);
            boolean hasResultSet = preparedStatement.execute();
            if (hasResultSet) {
                log.info("Task found in the replica table");
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    columnValue = rs.getString(columnName);
                }
            } else {
                log.error("Task not found in the replica table");
            }

        } catch (Exception e) {
            log.error("select query to get " + columnName + " failed  : {}, {} ",
                      e.getCause(), e.getMessage()
            );
        }
        return columnValue;
    }

    private boolean checkIfColumnAddedInTable(String tableName, String columnName,
                                                     JdbcDatabaseContainer container) throws SQLException {
        String selectQuery = "select " + columnName + " from cft_task_db." + tableName + " LIMIT 1 ";
        boolean columnAdded = false;
        try (Connection conn = DriverManager.getConnection(
            container.getJdbcUrl(),
            container.getUsername(),
            container.getPassword()
        );
             PreparedStatement preparedStatement = conn.prepareStatement(selectQuery)) {

            boolean hasResultSet = preparedStatement.execute();
            if (hasResultSet) {
                columnAdded = true;
            }
        } catch (PSQLException e) {
            if (e.getMessage().contains("column \"" + columnName + "\" does not exist")) {
                log.error("Column " + columnName + " not found in the table " + tableName);
                columnAdded = false;
            }
        } catch (Exception e) {
            log.error("select query to get " + columnName + " failed  : {}, {} ",
                      e.getCause(), e.getMessage()
            );
        }
        return columnAdded;
    }
}
