package net.hmcts.taskperf.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.hmcts.taskperf.model.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;

public class RoleAssignmentHelper
{
	/**
	 * Removes role assignments which cannot match the query filter.  For example, if the query filter
	 * has jurisdiction: [IA, SSCS], then a role assignment with jurisdiction = CIVIL cannot result
	 * in any tasks being added to the query result set, whereas a role assignment with jurisdiction =
	 * IA, SSCS or null can.
	 */
	public static Set<RoleAssignment> filterRoleAssignments(SearchRequest searchRequest)
	{
		return
				filterByAttribute(
					filterByAttribute(
						filterByAttribute(
							filterByAttribute(
									searchRequest.getUser().getRoleAssignments().stream(),
									RoleAttributeDefinition.JURISDICTION,
									searchRequest.getQuery().getFilter().getJurisdictions()),
							RoleAttributeDefinition.REGION,
							searchRequest.getQuery().getFilter().getRegions()),
						RoleAttributeDefinition.BASE_LOCATION,
						searchRequest.getQuery().getFilter().getLocations()),
					RoleAttributeDefinition.CASE_ID,
					searchRequest.getQuery().getFilter().getCaseIds())
				.filter(RoleAssignmentHelper::isTaskAccessGrantType)
				.filter(RoleAssignmentHelper::hasJurisdictionAttribute)
				.collect(Collectors.toSet());
	}

	/**
	 * Return just the role assignments which are of grant type EXCLUDED.
	 */
	public static Set<RoleAssignment> exclusionRoleAssignments(SearchRequest searchRequest)
	{
		return
				// No need to include exclusions that are for a jurisdiction that
				// isn't being queried.
				filterByAttribute(
						searchRequest.getUser().getRoleAssignments().stream(),
						RoleAttributeDefinition.JURISDICTION,
						searchRequest.getQuery().getFilter().getJurisdictions())
				.filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
				.filter(RoleAssignmentHelper::hasJurisdictionAttribute)
				.collect(Collectors.toSet());
	}


	/**
	 * Returns true only for role assignments of the grant types which can give
	 * access to tasks: STANDARD, SPECIFIC and CHALLENGED.
	 */
	private static boolean isTaskAccessGrantType(RoleAssignment roleAssignment)
	{
		GrantType grantType = roleAssignment.getGrantType();
		return
				grantType == GrantType.STANDARD ||
				grantType == GrantType.SPECIFIC ||
				grantType == GrantType.CHALLENGED;
	}

	/**
	 * Returns true if the role assignment has a non-null jurisdiction.
	 */
	public static boolean hasJurisdictionAttribute(RoleAssignment roleAssignment)
	{
		String jurisdiction = roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value());
		return jurisdiction != null && jurisdiction.trim().length() != 0;
	}

	/**
	 * Filters out role assignments which cannot match the constraints on the given attribute.  If the set of values
	 * is empty, then the attribute is unconstrained and nothing is removed.  If there are values provided, then
	 * the role assignment value must be one of the set, or null, otherwise the role assignment cannot match any
	 * tasks which also match the value set, and it is removed from consideration.
	 */
	private static Stream<RoleAssignment> filterByAttribute(Stream<RoleAssignment> roleAssignments, RoleAttributeDefinition attribute, Set<String> values)
	{
		return
				values == null || values.isEmpty() ?
				roleAssignments :
				roleAssignments.filter(ra -> roleAssignmentMatches(ra, attribute, values));
	}

	/**
	 * Returns true if the specified attribute is null (unconstrained) or is in the given set.
	 */
	private static boolean roleAssignmentMatches(RoleAssignment roleAssignment, RoleAttributeDefinition attribute, Set<String> values)
	{
		String attributeValue = roleAssignment.getAttributes().get(attribute.value());
		return attributeValue == null || values.contains(attributeValue);
	}

	/**
	 * Create a list of all the signatures representing the given role assignments with
	 * the given set of permissions. Multiple signatures are generated per role assignment.
	 */
	public static Set<String> buildRoleSignatures(Set<RoleAssignment> roleAssignments, Set<String> permissions)
	{
		Set<String> roleSignatures = new HashSet<>();
		for (RoleAssignment roleAssignment : roleAssignments)
		{
			for (String permission : permissions)
			{
				addRoleSignatures(roleAssignment, permission, roleSignatures);
			}
		}
		return roleSignatures;
	}

	/**
	 * Extend the collection with a "*" wildcard value.
	 */
	private static Collection<String> withWildcard(Collection<String> strings)
	{
		strings = (strings == null) ? new HashSet<>() : new HashSet<>(strings);
		strings.add("*");
		return strings;
	}

	/**
	 * If the value is null, replace it with a wildcard (*).
	 */
	private static String wildcardIfNull(String value)
	{
		return value == null ? "*" : value;
	}

	private static String[] UNKNOWN = new String[] {};
	private static String[] PUBLIC = new String[] {"U"};
	private static String[] PRIVATE = new String[] {"U", "P"};
	private static String[] RESTRICTED = new String[] {"U", "P", "R"};

	/**
	 * Return abbreviations for all the classification equal to or lower
	 * than the argument.  Abbreviations match the single-letter abbreviations
	 * used in the database to index tasks.
	 */
	private static String[] lowerClassifications(Classification classification)
	{
		switch (classification)
		{
		case PUBLIC:
			return PUBLIC;
		case PRIVATE:
			return PRIVATE;
		case RESTRICTED:
			return RESTRICTED;
		default:
			return UNKNOWN;
		}
	}

	/**
	 * Add all the role signatures to the list which can be created by the Cartesian product of:
	 *     - a pair of each singular attribute value and a wildcard (*)
	 *     - all the classifications <= the role assignment classification
	 *     - all the authorisations on the role assignment, plus a wildcard (*)
	 */
	private static void addRoleSignatures(RoleAssignment roleAssignment, String permission, Set<String> roleSignatures)
	{
		for (String classification : lowerClassifications(roleAssignment.getClassification()))
		{
			for (String authorisation : withWildcard(roleAssignment.getAuthorisations()))
			{
				String roleSignature = makeRoleSignature(roleAssignment, classification, authorisation, permission);
				if (roleSignature != null) roleSignatures.add(roleSignature);
			}
		}
	}

	/**
	 * Create the signature of the given role assignment, combined with the classification,
	 * authorisation and permission.  This matches the signatures used in the database to
	 * index tasks based on task role / permission configuration.
	 */
	private static String makeRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		if (treatAsOrganisationalRole(roleAssignment))
		{
			return makeOrganisationalRoleSignature(roleAssignment, classification, authorisation, permission);
		}
		else if (treatAsCaseRole(roleAssignment))
		{
			return makeCaseRoleSignature(roleAssignment, classification, authorisation, permission);
		}
		else
		{
			System.err.println("IGNORING UNSUPPORTED ROLE ASSIGNMENT " + roleAssignment.getRoleName());
			return null;
		}
	}

	/**
	 * Determine whether this role assignment should be treated as an organisational role.
	 */
	private static boolean treatAsOrganisationalRole(RoleAssignment roleAssignment)
	{
		return roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) == null;
	}

	/**
	 * Determine whether this role assignment should be treated as a case role.
	 */
	private static boolean treatAsCaseRole(RoleAssignment roleAssignment)
	{
		return roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) != null;
	}

	/**
	 * Create the signature of the given case role assignment, combined with the
	 * classification, authorisation and permission.  This matches the signatures used in
	 * the database to index tasks based on task role / permission configuration.
	 */
	private static String makeCaseRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		return
				"c:" +
				roleAssignment.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()) + ":" +
				roleAssignment.getRoleName() + ":" +
				permission + ":" +
				classification + ":" +
				authorisation;
	}

	/**
	 * Create the signature of the given organisational role assignment, combined with the
	 * classification, authorisation and permission.  This matches the signatures used in
	 * the database to index tasks based on task role / permission configuration.
	 */
	private static String makeOrganisationalRoleSignature(RoleAssignment roleAssignment, String classification, String authorisation, String permission)
	{
		return
				"o:" +
				roleAssignment.getAttributes().get(RoleAttributeDefinition.JURISDICTION.value()) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.REGION.value())) + ":" +
				wildcardIfNull(roleAssignment.getAttributes().get(RoleAttributeDefinition.BASE_LOCATION.value())) + ":" +
				roleAssignment.getRoleName() + ":" +
				permission + ":" +
				classification + ":" +
				authorisation;
	}
}