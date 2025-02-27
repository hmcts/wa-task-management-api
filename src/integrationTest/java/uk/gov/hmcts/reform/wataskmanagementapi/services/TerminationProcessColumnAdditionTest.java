package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.postgresql.util.PSQLException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
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
    void should_verify_if_termination_process_column_added_in_tables() {

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {

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
