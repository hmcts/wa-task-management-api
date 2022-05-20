package net.hmcts.taskperf.service.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Simple wrapper for dynamically building an SQL query and capturing
 * parameters, then executing as a prepared statement.
 *
 * It is the caller's responsibility to ensure that there is exactly one
 * "?" in the query for every parameter, and that they are added in the
 * right order.
 *
 * Parameters which are of type List or Set are added to the query as
 * SQL arrays, using the underlying connection to convert to the
 * native database array format.
 *
 * Also supports calling "explain analyze" for the query, and returning
 * the query plan as a list of strings.
 */
public class SqlStatement
{
	private StringBuilder sql = new StringBuilder();
	private List<Object> parameters = new ArrayList<>();

	public SqlStatement append(String text)
	{
		sql.append(text);
		return this;
	}

	public void addParameter(Object parameter)
	{
		parameters.add(parameter);
	}

	@SuppressWarnings("rawtypes")
	public void execute(Connection connection, Consumer<ResultSet> resultConsumer) throws SQLException
	{
		System.out.println("Executing SQL:");
		System.out.println(sql.toString());
		try (PreparedStatement statement = connection.prepareStatement(sql.toString()))
		{
			System.out.println("Parameters:");
			int parameterIndex = 1;
			for (Object parameter : parameters)
			{
				System.out.println(parameterIndex + " : " + parameter);
				if (parameter instanceof List)
				{
					statement.setArray(parameterIndex, connection.createArrayOf("text", ((List)parameter).toArray()));
				}
				else if (parameter instanceof Set)
				{
					statement.setArray(parameterIndex, connection.createArrayOf("text", ((Set)parameter).toArray()));
				}
				else
				{
					statement.setObject(parameterIndex, parameter);
				}
				++parameterIndex;
			}
			try (ResultSet results = statement.executeQuery())
			{
				while (results.next())
				{
					resultConsumer.accept(results);
				}
			}
		}
	}


	@SuppressWarnings("rawtypes")
	public List<String> explain(Connection connection) throws SQLException
	{
		List<String> explanation = new ArrayList<>();
		System.out.println("Explaining SQL:");
		System.out.println(sql.toString());
		try (PreparedStatement statement = connection.prepareStatement("explain analyze " + sql.toString()))
		{
			System.out.println("Parameters:");
			int parameterIndex = 1;
			for (Object parameter : parameters)
			{
				System.out.println(parameterIndex + " : " + parameter);
				if (parameter instanceof List)
				{
					statement.setArray(parameterIndex, connection.createArrayOf("text", ((List)parameter).toArray()));
				}
				else if (parameter instanceof Set)
				{
					statement.setArray(parameterIndex, connection.createArrayOf("text", ((Set)parameter).toArray()));
				}
				else
				{
					statement.setObject(parameterIndex, parameter);
				}
				++parameterIndex;
			}
			try (ResultSet results = statement.executeQuery())
			{
				while (results.next())
				{
					explanation.add(results.getString(1));
				}
			}
		}
		return explanation;
	}
}
