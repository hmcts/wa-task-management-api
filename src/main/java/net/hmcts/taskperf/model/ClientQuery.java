package net.hmcts.taskperf.model;

import java.util.ArrayList;
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

	/**
	 * Enhances the sort criteria with default sorting by major priority,
	 * priority date and minor priority.  These are added after any
	 * client-specified sorting criteria.
	 */
	public List<SortBy> getSortWithDefaults()
	{
		List<SortBy> newSort = new ArrayList<>(sort);
		addDefaultIfNeeded(newSort, SortBy.Column.MAJOR_PRIORITY);
		addDefaultIfNeeded(newSort, SortBy.Column.PRIORITY_DATE_TIME);
		addDefaultIfNeeded(newSort, SortBy.Column.MINOR_PRIORITY);
		return newSort;
	}

	/**
	 * Add sorting by the given column, as long as the column is not already
	 * specified in the sorting criteria.
	 */
	private void addDefaultIfNeeded(List<SortBy> newSort, SortBy.Column column)
	{
		if (!alreadyContains(column))
		{
			newSort.add(new SortBy(SortBy.Column.MAJOR_PRIORITY, SortBy.Direction.ASC));
		}
	}

	/**
	 * Does the sort already reference the given column?
	 */
	private boolean alreadyContains(SortBy.Column column)
	{
		return
				sort == null ?
				false :
				sort.stream().anyMatch(sb -> sb.getColumn() == column);
	}
}
