package net.hmcts.taskperf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.hmcts.taskperf.loader.Loader;
import net.hmcts.taskperf.model.ClientQuery;
import net.hmcts.taskperf.model.SearchRequest;
import net.hmcts.taskperf.model.User;
import net.hmcts.taskperf.service.TaskSearch;
import net.hmcts.taskperf.service.TaskSearch.Event;
import net.hmcts.taskperf.service.TaskSearch.Task;

public class Main
{
	public static String DB_HOST = System.getenv("CFT_TASK_DB_HOST");
	public static boolean EXPLAIN_QUERIES = ("Y".equalsIgnoreCase(System.getenv("EXPLAIN_QUERIES")));
	private static String TASK_DB_JDBC_URL = "jdbc:postgresql://" + DB_HOST + "/postgres";
	private static String TASK_DB_USER = "postgres";
	private static String TASK_DB_PASSWORD = "postgres";

	private static Connection getTaskDbConnection()
	{
		try
		{
			return DriverManager.getConnection(TASK_DB_JDBC_URL, TASK_DB_USER, TASK_DB_PASSWORD);
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception
	{
		if (DB_HOST == null)
		{
			System.err.println("Please set environment variable CFT_TASK_DB_HOST = host_address:postgres_port");
			System.exit(1);
		}
		String userId = args[0];
		String searchId = args[0];
		User user = Loader.loadUser(userId);
		ClientQuery clientQuery = Loader.loadClientQuery(searchId);
		try (Connection connection = getTaskDbConnection())
		{
			TaskSearch.Results searchResults = TaskSearch.searchTasks(clientQuery, user.getRoleAssignments(), connection, EXPLAIN_QUERIES);
			System.out.println("Results\n*******");
			System.out.println("Count : " + searchResults.getTotalCount());
			for (Task task : searchResults.getTasks())
			{
				System.out.println(task.getAttributes());
			}
			System.out.println("Query Plans\n***********");
			for (String line : searchResults.queryPlans)
			{
				System.out.println(line);
			}
			System.out.println("Timings\n********");
			for (Event event : searchResults.log)
			{
				String time = "" + event.getTime();
				while (time.length() < 6) time = " " + time;
				System.out.println(time + " : " + event.getDescription());
			}
		}
	}
}
