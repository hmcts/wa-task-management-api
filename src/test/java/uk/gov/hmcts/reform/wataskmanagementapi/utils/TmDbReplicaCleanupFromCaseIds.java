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

public class TmDbReplicaCleanupFromCaseIds {

    public static void main(String[] args) {
        // Path to the CSV file
        String csvFilePath = "/Users/adityadwadasi/Documents/Code/wa-task-management-api/src/main/java/case_data_202509181137_demo_List.csv"; // Replace with your CSV file path

        String dbUrlReplica = "jdbc:postgresql://<host_name>:5432/ccd_data_store"; // Replace with your DB URL
        String dbUserReplica = "pgadmin"; // Replace with your DB username
        String dbPasswordReplica = "<db_password>"; // Replace with your DB password

        try {
            // Read IDs from the CSV file
            List<String> caseIds = readIDsFromFile(csvFilePath);

            System.out.println(caseIds.size() + " ids read from the file.");

            for (String caseId : caseIds) {

                System.out.println("Processing case id: " + caseId);

                // Connect to PostgreSQL and execute the query
                try (Connection connection = DriverManager.getConnection(dbUrlReplica, dbUserReplica, dbPasswordReplica)) {

                    // get taskId from caseId
                    String taskId = getTaskIdFromCaseId(connection,caseId);
                    if (StringUtils.isNotEmpty(taskId)) {
                        deleteTaskAssignmentsReplica(connection, taskId);
                        deleteReportableTaskReplica(connection, taskId);
                        deleteTaskHistoryReplica(connection, taskId);
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
        String sql = "SELECT task_id FROM cft_task_db.reportable_task WHERE case_id = ?";
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

    // Deletes from reportable_task for the given taskId
    private static void deleteReportableTaskReplica(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.reportable_task WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

    // Deletes from task_assignments for the given taskId
    private static void deleteTaskAssignmentsReplica(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.task_assignments WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

    private static void deleteTaskHistoryReplica(Connection connection, String taskId) throws SQLException {
        String sql = "DELETE FROM cft_task_db.task_history WHERE task_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, taskId);
            stmt.executeUpdate();
        }
    }

}
