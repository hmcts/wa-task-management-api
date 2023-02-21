package net.hmcts.taskperf;

import org.flywaydb.core.Flyway;

public class SetupDB
{
	public static String URL = "jdbc:posgresql://localhost:5432/postgres";
	public static String USER = "postgres";
	public static String PASSWORD = "postgres";
	
	public static void main(String[] args) throws Exception
	{
		Flyway flyway = Flyway.configure().dataSource(Main.TASK_DB_JDBC_URL, Main.TASK_DB_USER, Main.TASK_DB_PASSWORD).defaultSchema("cft_task_db").load();
		flyway.migrate();
	}
}
