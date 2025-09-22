package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TmDbPrimaryCleanupFromCaseIds {

    public static void main(String[] args) {
        // Path to the CSV file
        String csvFilePath = "/Users/adityadwadasi/Documents/Code/wa-task-management-api/src/main/java/case_data_202509181137_demo_List.csv"; // Replace with your CSV file path

        // PostgreSQL connection details
        String dbUrl = "jdbc:postgresql://<host_name>:5432/ccd_data_store"; // Replace with your DB URL
        String dbUser = "pgadmin"; // Replace with your DB username
        String dbPassword = "<db_password>"; // Replace with your DB password

        try {
            // Read IDs from the CSV file
            List<String> caseIds = readIDsFromFile(csvFilePath);

            System.out.println(caseIds.size() + " ids read from the file.");

            for (String caseId : caseIds) {

                System.out.println("Processing case id: " + caseId);

                // Connect to PostgreSQL and execute the query
                try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

                    // get taskId from caseId
                    String taskId = getTaskIdFromCaseId(connection,caseId);
                    if (StringUtils.isNotEmpty(taskId)) {
                        deleteSensitiveTaskEventLogs(connection, taskId);
                        deleteTaskRoles(connection, taskId);
                        deleteTask(connection, taskId);
                    } else {
                        System.out.println("No taskId found for caseId: " + caseId);
                    }
                }
                catch(SQLException e){
                    System.err.println("Database error: " + e.getMessage());
                }
            }
        } catch(IOException e){
            System.err.println("Error reading the CSV file: " + e.getMessage());
        }
    }

    private static String getTaskIdFromCaseId(Connection connection, String caseId) throws SQLException {
        String sql = "SELECT task_id FROM cft_task_db.tasks WHERE case_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, caseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("task_id");
                }
            }
        }
        return null; // Not found
    }


    private static List<String> readIDsFromFile(String filePath) throws IOException {
        // Read all lines from the file into a List
        return Files.readAllLines(Paths.get(filePath));
    }

    // Deletes from sensitive_task_event_logs for the given taskId
    private static void deleteSensitiveTaskEventLogs(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.sensitive_task_event_logs WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

    // Deletes from task_roles for the given taskId
    private static void deleteTaskRoles(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.task_roles WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

    // Deletes from tasks for the given taskId
    private static void deleteTask(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.tasks WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

}
