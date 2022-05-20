package net.hmcts.taskperf.model;

import java.util.List;

import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

/**
 * A representation of the search query provided by the client.
 * Consists of a set of filter criteria, a specification of the
 * pagination required for the results, and a set of sort
 * criteria.
 */
@Value
public class ClientQuery
{
	private List<SearchParameterList> listFilters;
	private List<SearchParameterBoolean> booleanFilters;
	private Pagination pagination;
	private List<SortingParameter> sort;
}
