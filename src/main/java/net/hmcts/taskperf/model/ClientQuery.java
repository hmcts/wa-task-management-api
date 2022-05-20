package net.hmcts.taskperf.model;

import java.util.List;

import lombok.Value;

/**
 * A representation of the search query provided by the client.
 * Consists of a set of filter criteria, a specification of the
 * pagination required for the results, and a set of sort
 * criteria.
 */
@Value
public class ClientQuery
{
	private ClientFilter filter;
	private Pagination pagination;
	private List<SortBy> sort;
}
