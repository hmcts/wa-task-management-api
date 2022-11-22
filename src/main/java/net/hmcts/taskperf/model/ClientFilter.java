package net.hmcts.taskperf.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

/**
 * The filter criteria received from the client in a search request.
 * All the values included are needed to support current functionality,
 * except for region, which may be added in the future.  It is not
 * believed that we have any current requirement to support multiple
 * values for case ID or assignee, but this is supported for
 * completeness.
 */
@Value
public class ClientFilter
{
	private Set<String> states;
	private Set<String> jurisdictions;
	private Set<String> roleCategories;
	private Set<String> workTypes;
	private Set<String> taskTypes;
	private Set<String> regions;
	private Set<String> locations;
	private Set<String> caseIds;
	private Set<String> assignees;
	private boolean availableTasksOnly;
	private boolean allWork;

	public static ClientFilter of(ClientQuery clientQuery)
	{
		List<SearchParameter<?>> searchParameters = new ArrayList<>();
		if (clientQuery.getListFilters() != null) searchParameters.addAll(clientQuery.getListFilters());
		if (clientQuery.getBooleanFilters() != null) searchParameters.addAll(clientQuery.getBooleanFilters());
		return new ClientFilter(
				getConstraints(searchParameters, SearchParameterKey.STATE),
				getConstraints(searchParameters, SearchParameterKey.JURISDICTION),
				getConstraints(searchParameters, SearchParameterKey.ROLE_CATEGORY),
				getConstraints(searchParameters, SearchParameterKey.WORK_TYPE),
				getConstraints(searchParameters, SearchParameterKey.TASK_TYPE),
				new HashSet<>(), // region not yet required in filter.
				getConstraints(searchParameters, SearchParameterKey.LOCATION),
				getConstraints(searchParameters, SearchParameterKey.CASE_ID),
				getConstraints(searchParameters, SearchParameterKey.USER),
				availableTasksOnly(clientQuery),
        allWork(clientQuery));
	}

	private static boolean availableTasksOnly(ClientQuery clientQuery)
	{
		return clientQuery.getRequestContext() != null
            && clientQuery.getRequestContext().isEqual(RequestContext.AVAILABLE_TASK_ONLY);
	}

    private static boolean allWork(ClientQuery clientQuery) {
        return clientQuery.getRequestContext() != null
            && clientQuery.getRequestContext().isEqual(RequestContext.ALL_WORK);
    }


	/**
	 * This is a bit involved.  A list of search parameters could contain multiple values for the
	 * same key - not sure if the API checks for that.  Since the parameters are ANDed for the query,
	 * we need to find the intersection of all the sets of values for a single parameter, allowing
	 * for the fact that they could be single-valued or multi-valued.
	 *
	 * TODO: need to establish the variants of search parameters which can actually be received from
	 * the API.  e.g. is there an EQUALS operator with a single value, or are the values always in
	 * a list?
	 *
	 * This probably works for simple cases, which may be enough for indicative performance testing.
	 */
	private static Set<String> getConstraints(List<SearchParameter<?>> searchParameters, SearchParameterKey searchParameterKey)
	{
		List<Set<String>> constraintsList = new ArrayList<>();
		for (SearchParameter<?> searchParameter : searchParameters)
		{
			if (searchParameter.getKey().equals(searchParameterKey))
			{
				if (!searchParameter.getOperator().equals(SearchOperator.IN))
				{
					throw new UnsupportedOperationException("ClientFilter only supports IN operators.  Received " + searchParameter.getOperator());
				}
				if (!(searchParameter instanceof SearchParameterList))
				{
					throw new UnsupportedOperationException("ClientFilter only supports lists of values.  Received " + searchParameter);
				}
				List<String> values = ((SearchParameterList)searchParameter).getValues();
				Set<String> constraints = new HashSet<>(values);
				constraintsList.add(constraints);
			}
		}
		// Load the first set into mergedConstraints, then filter to retain
		// only the elements common with all the others.  This will usually
		// only find one set of values.
		Set<String> mergedConstraints = new HashSet<>();
		boolean firstRun = true;
		for (Set<String> constraints : constraintsList)
		{
			if (firstRun)
			{
				mergedConstraints.addAll(constraints);
				firstRun = false;
			}
			else
			{
				mergedConstraints.retainAll(constraints);
			}
		}
		/*
		 * If we have more than one occurrence of the parameter in the query, but we ended up
		 * with no values in the list, then we have a query which cannot return any results.
		 * We need to trap this, because a client could automatically add constraints to the
		 * user's query, which the user could then remove by inputting a conflicting constraint -
		 * we can't just proceed to run the query with an empty constraint for the parameter.
		 */
		if (constraintsList.size() > 1 && mergedConstraints.isEmpty())
		{
			throw new RuntimeException("Two incompatible lists of values received for " + searchParameterKey + ". " +
					"Should probably be properly trapped and result in a BAD REQUEST response.");
		}
		return mergedConstraints;
	}
}
