package net.hmcts.taskperf.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Value;
import net.hmcts.taskperf.model.ClientFilter;
import net.hmcts.taskperf.model.ClientQuery;
import net.hmcts.taskperf.model.Pagination;
import net.hmcts.taskperf.model.SearchRequest;
import net.hmcts.taskperf.service.sql.SqlStatement;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

/**
 * Main process for performing a task search.
 */
public class TaskSearch
{
	@Value
	public static class Results
	{
		public final int totalCount;
		public final List<Task> tasks;
		public final List<String> queryPlans;
		public final List<Event> log;
	}

	@Value
	public static class Task
	{
		private Map<String, Object> attributes;
	}

	@Value
	public static class Event
	{
		private String description;
		private long time;
	}

	/**
	 * System timestamp when the search was initiated.
	 */
	private final long startTime;

	/**
	 * The client query object built from the search request.
	 */
	private final ClientFilter clientFilter;

	/**
	 * Keep track of the timings.
	 */
	private final List<Event> log = new ArrayList<>();

	/**
	 * Determines whether to explain the queries as well as running them.
	 */
	private final boolean explainQueries;

	/**
	 * Determines whether to analyse the queries as well as running them.
	 */
	private final List<String> queryPlans = new ArrayList<>();

	/**
	 * Total number of tasks matching the search.
	 */
	private int totalCount;

	/**
	 * The tasks returned from the search
	 */
	private final List<Task> tasks = new ArrayList<>();

	/**
	 * The permissions the user must have for tasks returned.
	 */
	private final Set<String> permissions;

	/**
	 * The SQL statement to retrieve the requested page of tasks.
	 */
	private final SqlStatement searchStatement = new SqlStatement();

	/**
	 * The SQL statement to retrieve the count of tasks matching the search.
	 */
	private final SqlStatement countStatement = new SqlStatement();

	/**
	 * The signatures of the  role assignments which need to be matched.
	 */
	private Set<String> roleSignatures = new HashSet<>();

	/**
	 * The signatures of the  filter criteria which need to be matched.
	 */
	private Set<String> filterSignatures = new HashSet<>();

	/**
	 * Extra constraint clauses to be added to the SQL.
	 */
	private String extraConstraints = "";

	/**
	 * Parameters to accompany the extra constraint clauses.
	 */
	private final List<Object> extraConstraintParameters = new ArrayList<>();

	/**
	 * The order by criteria for the search.
	 */
	private final List<SortingParameter> orderBy;

	/**
	 * The offset to request in the SQL.
	 */
	private int offset;

	/**
	 * The limit to request in the SQL.
	 */
	private int limit;

	public static Results searchTasks(ClientQuery clientQuery, List<RoleAssignment> roleAssignments, Connection connection, boolean explainQueries) throws SQLException
	{
		TaskSearch taskSearch = new TaskSearch(clientQuery, roleAssignments, explainQueries);
		taskSearch.run(connection);
		return new Results(taskSearch.totalCount, taskSearch.tasks, taskSearch.queryPlans, taskSearch.log);
	}

	private TaskSearch(ClientQuery clientQuery, List<RoleAssignment> roleAssignments, boolean explainQueries)
	{
		this.startTime = System.currentTimeMillis();
		log("Started");
		this.explainQueries = explainQueries;
		this.clientFilter = ClientFilter.of(clientQuery);
		Set<RoleAssignment> filteredroleAssignments = RoleAssignmentHelper.filterRoleAssignments(roleAssignments, clientFilter);
		this.permissions = buildPermissions(clientFilter);
		this.roleSignatures = RoleAssignmentHelper.buildRoleSignatures(filteredroleAssignments, permissions);
		this.filterSignatures = FilterHelper.buildFilterSignatures(clientFilter);
		buildExtraConstraints(filteredroleAssignments, SEARCH_SQL_TASK_ALIAS);
		Pagination pagination = clientQuery.getPagination();
		offset = pagination.getFirstResult();
		limit = pagination.getMaxResults();
		orderBy = clientQuery.getSort();
		log("Starting to build queries");
		buildSearchSqlStatement();
		log("Search query built");
		buildCountSqlStatement();
		log("Count query built");
	}

	private void log(String description)
	{
		log.add(new Event(description, System.currentTimeMillis() - startTime));
	}

	private void run(Connection connection) throws SQLException
	{
		if (explainQueries)
		{
			log("Explaining search");
			explainSearch(connection);
			log("Finished explaining search");
			log("Explaining count");
			explainCount(connection);
			log("Finished explaining count");
		}
		log("Starting search");
		runSearch(connection);
		log("Finished search");
		log("Starting count");
		runCount(connection);
		log("Finished count");
	}

	private static Set<String> buildPermissions(ClientFilter clientFilter)
	{
		Set<String> permissions = new HashSet<>();
		if (clientFilter.isAvailableTasksOnly())
		{
			// TODO: should be changed to use the claim permission, once available
			// TODO: currently removes the permission requested when it is not one
			//       of the 'available' permissions.  Arguably this should return
			//       tasks where the user has BOTH any of the available task
			//       permissions AND any of the requested permissions.  But that
			//       makes the query complex - we should probably not expose
			//       the ability to request specific permissions through the API,
			//       but flags/properties for particular types of query, which
			//       we then interpret to determine what permissions we should
			//       search for.  (There are other reasons for this, too - we
			//       don't want to have to index on all the permissions, just
			//       the ones we need for the search scenarios we have to support.
			permissions.add("o");
//			permissions.add("x");
		}
		else
		{
			permissions.add("r");
		}
		return permissions;
	}

	/**
	 * The SQL for doing the actual search for tasks.
	 */
	private static final String SEARCH_SQL = "" +
		    "select t.task_id\n" +
		    "from   cft_task_db.tasks t\n" +
		    "where  indexed\n" +
		    "and    state in ('ASSIGNED','UNASSIGNED')\n" +
		    "and    cft_task_db.filter_signatures(t.task_id) && ?\n" +
		    "and    cft_task_db.role_signatures(t.task_id) && ?[EXTRA_CONSTRAINTS]\n" +
		    "order by [ORDER_BY]t.major_priority desc, t.priority_date_time desc, t.minor_priority desc\n" +
		    "offset ? limit ?";

	private static final String SEARCH_SQL_TASK_ALIAS = "t";

	private void buildSearchSqlStatement()
	{
		String sql = SEARCH_SQL;
		sql = addOrderBy(sql, orderBy, SEARCH_SQL_TASK_ALIAS);
		sql = addExtraConstraints(sql, extraConstraints);
		searchStatement.append(sql);
		searchStatement.addParameter(filterSignatures);
		searchStatement.addParameter(roleSignatures);
		for (Object parameter : extraConstraintParameters)
		{
			searchStatement.addParameter(parameter);
		}
		searchStatement.addParameter(offset);
		searchStatement.addParameter(limit);
	}

	private void buildExtraConstraints(Set<RoleAssignment> roleAssignments, String alias)
	{
		buildExtraConstraint(clientFilter.getCaseIds(), alias, "case_id", true);
		buildExtraConstraint(clientFilter.getAssignees(), alias, "assignee", true);
		buildExtraConstraint(clientFilter.getTaskTypes(), alias, "task_type", true);
		buildExtraConstraint(clientFilter.getTaskIds(), alias, "task_id", true);
		buildExtraConstraint(
				buildExcludedCaseIds(RoleAssignmentHelper.exclusionRoleAssignments(roleAssignments, clientFilter)),
				alias, "case_id", false);
		buildAvailableTasksOnlyConstraint(alias);
	}

	private void buildExtraConstraint(Set<String> values, String alias, String column, boolean include)
	{
		if (values != null && !values.isEmpty())
		{
			if (values.size() == 1)
			{
				extraConstraints += "\nand " + alias + "." + column + (include ? " = " : " <> ") + "?";
				extraConstraintParameters.add(values.stream().findFirst().get());
			}
			else
			{
				extraConstraints += "\nand " + (include ? "" : "not ") + alias + "." + column + " = any(?)";
				extraConstraintParameters.add(values);
			}
		}
	}

	private void buildAvailableTasksOnlyConstraint(String alias)
	{
		if (clientFilter.isAvailableTasksOnly())
		{
			extraConstraints += "\nand " + alias + ".assignee is null";
		}
	}

	private Set<String> buildExcludedCaseIds(Set<RoleAssignment> roleAssignments)
	{
		return
				roleAssignments.stream()
				.filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
				.map(ra -> ra.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()))
				.filter(c -> c != null)
				.collect(Collectors.toSet());
	}

	private static String addOrderBy(String sql, List<SortingParameter> sort, String alias)
	{
		String orderColumns = "";
		if (sort != null)
		{
			for (SortingParameter sortBy : sort)
			{
				orderColumns += alias + "." + getSortColumn(sortBy.getSortBy()) + " " + sortBy.getSortOrder().toString() + ", ";
			}
		}
		return sql.replace("[ORDER_BY]", orderColumns);
	}

	private static final Map<String, String> SORT_COLUMNS =
			Stream.of(new String[][] {
				{ "duedate", "due_date_time" },
				{ "due_date", "due_date_time" },
				{ "tasktitle", "task_title" },
				{ "task_title", "task_title" },
				{ "locationname", "location_name" },
				{ "location_name", "location_name" },
				{ "casecategory", "case_category" },
				{ "case_category", "case_category" },
				{ "caseid", "case_id" },
				{ "case_id", "case_id" },
				{ "casename", "case_name" },
				{ "case_name", "case_name" }
			}).collect(Collectors.toMap(data -> data[0], data -> data[1]));

	private static final String getSortColumn(SortField sortField)
	{
		return SORT_COLUMNS.get(sortField.getId().toLowerCase());
	}

	private static String addExtraConstraints(String sql, String extraConstraints)
	{
		return sql.replace("[EXTRA_CONSTRAINTS]", extraConstraints);
	}

	private void runSearch(Connection connection) throws SQLException
	{
		searchStatement.execute(connection, r -> tasks.add(makeTask(r)));
	}

	private void explainSearch(Connection connection) throws SQLException
	{
		queryPlans.add("SEARCH QUERY");
		queryPlans.add("============");
		queryPlans.addAll(searchStatement.explain(connection));
	}

	private Task makeTask(ResultSet results)
	{
		try
		{
			Map<String, Object> attributes = new HashMap<>();
			ResultSetMetaData metadata = results.getMetaData();
			for (int i = 1; i <= metadata.getColumnCount(); ++i)
			{
				attributes.put(results.getMetaData().getColumnName(i), results.getObject(i));
			}
			return new Task(attributes);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * The SQL for counting the total of matching tasks.
	 */
	private static String COUNT_SQL = "" +
			"select count(*) as count\n" +
			"from   cft_task_db.tasks t\n" +
			"where  indexed\n" +
			"and    state in ('ASSIGNED','UNASSIGNED')\n" +
			"and    cft_task_db.filter_signatures(task_id) && ?\n" +
			"and    cft_task_db.role_signatures(task_id) && ?[EXTRA_CONSTRAINTS]";

	private void buildCountSqlStatement()
	{
		String sql = addExtraConstraints(COUNT_SQL, extraConstraints);
		countStatement.append(sql);
		countStatement.addParameter(filterSignatures);
		countStatement.addParameter(roleSignatures);
		for (Object parameter : extraConstraintParameters)
		{
			countStatement.addParameter(parameter);
		}
	}

	private void runCount(Connection connection) throws SQLException
	{
		countStatement.execute(connection, r -> totalCount = getCount(r));
	}

	private void explainCount(Connection connection) throws SQLException
	{
		queryPlans.add("COUNT QUERY");
		queryPlans.add("============");
		queryPlans.addAll(countStatement.explain(connection));
	}

	private int getCount(ResultSet results)
	{
		try
		{
			return results.getInt("count");
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
