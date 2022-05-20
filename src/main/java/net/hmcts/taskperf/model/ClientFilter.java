package net.hmcts.taskperf.model;

import java.util.Set;

import lombok.Value;

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
	private Set<String> regions;
	private Set<String> locations;
	private Set<String> caseIds;
	private Set<String> assignees;
	private Set<String> permissions;
}
