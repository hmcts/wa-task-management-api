package uk.gov.hmcts.reform.wataskmanagementapi.services.signature;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.CHALLENGED;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.SPECIFIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType.STANDARD;

public final class RoleSignatureBuilder {
    private static final String WILDCARD = "*";
    public static final String OWN_AND_CLAIM_PERMISSION = "a";
    public static final String MANAGE_PERMISSION = "m";
    public static final String READ_PERMISSION = "r";

    private RoleSignatureBuilder() {
        //Utility class constructor
    }

    /**
     * Create a list of all the signatures representing the given role assignments with
     * the given set of permissions. Multiple signatures are generated per role assignment.
     * Cartesian product of:
     * - a pair of each singular attribute value and a wildcard (*)
     * - all the classifications <= the role assignment classification
     * - all the authorisations on the role assignment, plus a wildcard (*)
     */

    public static Set<String> buildRoleSignatures(Collection<RoleAssignment> roleAssignments,
                                                  SearchRequest searchTaskRequest) {
        Set<RoleAssignment> filteredRoleAssignments = filterRoleAssignments(roleAssignments, searchTaskRequest);

        Set<String> roleSignatures = new HashSet<>();
        for (RoleAssignment roleAssignment : filteredRoleAssignments) {
            for (String authorisation : authorisations(roleAssignment, searchTaskRequest)) {
                String classification = roleAssignment.getClassification().getAbbreviation();
                String roleSignature = makeRoleSignature(roleAssignment, classification, authorisation,
                                                         permissionRequirement(searchTaskRequest));
                roleSignatures.add(roleSignature);
            }
        }
        return roleSignatures;
    }

    /**
     * Returns the list of authorisations to be included in signatures for the given role assignment.
     * If the query is looking for available tasks, then signatures for organisational roles include
     * all the user's authorisations, plus a wildcard ("*").
     * For other types of query, and for case roles in all queries, authorisations are ignored, and
     */
    private static List<String> authorisations(RoleAssignment roleAssignment, SearchRequest searchTaskRequest) {
        boolean isOrganisationalRole = roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).isEmpty();

        if (searchTaskRequest.isAvailableTasksOnly() && isOrganisationalRole) {
            return withWildcard(roleAssignment.getAuthorisations());
        } else {
            return List.of(WILDCARD);
        }
    }

    /**
     * Create the signature of the given role assignment, combined with the
     * classification, authorisation and permission.  This matches the signatures used in
     * the database to index tasks based on task role / permission configuration.
     * This is a uniform procedure that can be used for both case roles and organisational
     * roles.
     */
    private static String makeRoleSignature(RoleAssignment roleAssignment,
                                            String classification,
                                            String authorisation,
                                            String permission) {
        return String.join(":",
            wildcardIfNull(roleAssignment.getAttributeValue(RoleAttributeDefinition.JURISDICTION).orElse(null)),
            wildcardIfNull(roleAssignment.getAttributeValue(RoleAttributeDefinition.REGION).orElse(null)),
            wildcardIfNull(roleAssignment.getAttributeValue(RoleAttributeDefinition.BASE_LOCATION).orElse(null)),
            roleAssignment.getRoleName(),
            wildcardIfNull(roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).orElse(null)),
            permission,
            wildcardIfNull(classification),
            authorisation);
    }


    private static String permissionRequirement(SearchRequest searchTaskRequest) {
        if (searchTaskRequest.isAvailableTasksOnly()) {
            //'a' represent own and claim permission in role signature
            return OWN_AND_CLAIM_PERMISSION;
        } else if (searchTaskRequest.isAllWork()) {
            //'m' represent manage permission in role signature
            return MANAGE_PERMISSION;
        } else {
            //'r' represent read permission in role signature
            return READ_PERMISSION;
        }
    }

    /**
     * Removes role assignments which cannot match the query filter.  For example, if the query filter
     * has jurisdiction: [IA, SSCS], then a role assignment with jurisdiction = CIVIL cannot result
     * in any tasks being added to the query result set, whereas a role assignment with jurisdiction =
     * IA, SSCS or null can.
     */
    private static Set<RoleAssignment> filterRoleAssignments(Collection<RoleAssignment> roleAssignments,
                                                             SearchRequest searchTaskRequest) {
        return roleAssignments.stream()
            .filter(filterByAttribute(RoleAttributeDefinition.JURISDICTION, searchTaskRequest.getJurisdictions()))
            .filter(filterByAttribute(RoleAttributeDefinition.REGION, searchTaskRequest.getRegions()))
            .filter(filterByAttribute(RoleAttributeDefinition.BASE_LOCATION, searchTaskRequest.getLocations()))
            .filter(filterByAttribute(RoleAttributeDefinition.CASE_ID, searchTaskRequest.getCaseIds()))
            .filter(r -> List.of(STANDARD, SPECIFIC, CHALLENGED).contains(r.getGrantType()))
            .collect(Collectors.toSet());
    }

    /**
     * Filters out role assignments which cannot match the constraints on the given attribute.  If the set of values
     * is empty, then the attribute is unconstrained and nothing is removed.  If there are values provided, then
     * the role assignment value must be one of the set, or null, otherwise the role assignment cannot match any
     * tasks which also match the value set, and it is removed from consideration. Returns true if the specified
     * attribute is null (unconstrained) or is in the given set.
     */
    private static Predicate<RoleAssignment> filterByAttribute(RoleAttributeDefinition attribute, List<String> values) {
        return roleAssignment -> values == null || values.isEmpty()
                                 || roleAssignment.getAttributes().get(attribute.value()) == null
                                 || values.contains(roleAssignment.getAttributes().get(attribute.value()));
    }

    private static List<String> withWildcard(List<String> values) {
        List<String> valuesWithWildcard = new ArrayList<>();
        if (values != null) {
            valuesWithWildcard.addAll(values);
        }
        valuesWithWildcard.add(WILDCARD);
        return valuesWithWildcard;
    }

    private static String wildcardIfNull(String value) {
        return value == null ? WILDCARD : value;
    }

}
