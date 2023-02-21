package net.hmcts.taskperf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestConnection
{
	private static Connection getTaskDbConnection()
	{
		try
		{
			return DriverManager.getConnection(Main.TASK_DB_JDBC_URL, Main.TASK_DB_USER, Main.TASK_DB_PASSWORD);
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception
	{
		try (Connection connection = getTaskDbConnection())
		{
			try (PreparedStatement statement = connection.prepareStatement("select * from cft_task_db.tasks limit 10"))
			{
				try (ResultSet results = statement.executeQuery())
				{
					while (results.next())
					{
						System.out.println(results.getObject(1));
					}
				}
			}
		}
	}
}
