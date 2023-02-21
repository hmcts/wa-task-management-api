package net.hmcts.taskperf.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.hmcts.taskperf.model.ClientFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;

public class FilterHelper
{
	/**
	 * Create a list of all the signatures reprsenting the given filter.
	 * Multiple signatures are generated per filter.
	 */
	public static Set<String> buildFilterSignatures(ClientFilter filter)
	{
		Set<String> filterSignatures = new HashSet<>();
		for (String state : defaultToWildcard(abbreviateStates(filter.getStates())))
		{
			for (String jurisdiction : defaultToWildcard(filter.getJurisdictions()))
			{
				for (String roleCategory : defaultToWildcard(abbreviateRoleCategories(filter.getRoleCategories())))
				{
					for (String workType : defaultToWildcard(filter.getWorkTypes()))
					{
						for (String region : defaultToWildcard(filter.getRegions()))
						{
							for (String location : defaultToWildcard(filter.getLocations()))
							{
								filterSignatures.add(state + ":" + jurisdiction + ":" + roleCategory + ":" + workType + ":" + region + ":" + location);
							}
						}
					}
				}
			}
		}
		return filterSignatures;
	}

	private static final Set<String> WILDCARD = Collections.singleton("*");

	/**
	 * Default to a wildcard (*) if the set is null or empty.
	 */
	private static Collection<String> defaultToWildcard(Collection<String> strings)
	{
		return (strings == null || strings.isEmpty()) ? WILDCARD : strings;
	}

	/**
	 * Abbreviate a set of role category values to the single characters
	 * used in the database index.
	 */
	private static Set<String> abbreviateRoleCategories(Set<String> roleCategories)
	{
		Set<String> abbreviated = new HashSet<>();
		for (String roleCategory : roleCategories)
		{
			if (RoleCategory.JUDICIAL.name().equalsIgnoreCase(roleCategory))
			{
				abbreviated.add("J");
			}
			else if (RoleCategory.LEGAL_OPERATIONS.name().equalsIgnoreCase(roleCategory))
			{
				abbreviated.add("L");
			}
			else if (RoleCategory.ADMIN.name().equalsIgnoreCase(roleCategory))
			{
				abbreviated.add("A");
			}
			else if (RoleCategory.UNKNOWN.name().equalsIgnoreCase(roleCategory))
			{
				abbreviated.add("U");
			}
			else 
			{
				abbreviated.add(roleCategory);
			}
		}
		return abbreviated;
	}

	/**
	 * Abbreviate a set of state values to the single characters used
	 * in the database index.
	 */
	private static Set<String> abbreviateStates(Set<String> states)
	{
		Set<String> abbreviated = new HashSet<>();
		for (String state : states)
		{
			if ("ASSIGNED".equalsIgnoreCase(state))
			{
				abbreviated.add("A");
			}
			else if ("UNASSIGNED".equalsIgnoreCase(state))
			{
				abbreviated.add("U");
			}
			else
			{
				abbreviated.add(state);
			}
		}
		return abbreviated;
	}
}