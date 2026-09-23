package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import jakarta.persistence.Query;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.MANAGE_PERMISSION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.OWN_AND_CLAIM_PERMISSION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.READ_PERMISSION;

/**
 * Matches the original task-role rows without expanding permission or signature rows.
 * Role names are combined only when their task scope and authorizations are identical.
 */
record TaskRoleSearchPredicate(String sql, Map<String, Object> parameters) {

    private static final String WILDCARD = "*";
    // Keep these as SQL predicates so permission-specific partial indexes remain eligible.
    private static final Map<String, String> PERMISSION_PREDICATES = Map.of(
        READ_PERMISSION, "tr.read",
        MANAGE_PERMISSION, "tr.manage",
        OWN_AND_CLAIM_PERMISSION, "tr.own AND tr.claim"
    );
    private static final String PERMISSION_EXISTS = """
        EXISTS (
            SELECT 1
            FROM {h-schema}task_roles tr
            WHERE tr.task_id = t.task_id
              AND %s
              AND tr.role_name IN (:%sroleNames)
              AND (%s)
            %s
        )
        """;
    private static final String ROLE_SCOPE = """
        (
            (CAST(:scope_jurisdiction AS text) IS NULL OR t.jurisdiction = CAST(:scope_jurisdiction AS text))
            AND (CAST(:scope_region AS text) IS NULL OR t.region = CAST(:scope_region AS text))
            AND (CAST(:scope_location AS text) IS NULL OR t.location = CAST(:scope_location AS text))
            AND (CAST(:scope_caseId AS text) IS NULL OR t.case_id = CAST(:scope_caseId AS text))
            AND t.security_classification = ANY(
                CAST(:scope_classifications AS {h-schema}security_classification_enum[])
            )
            AND tr.role_name IN (:scope_roleNames)
            %s
        )
        """;
    private static final String AUTHORIZATION_SCOPE = """
        AND (
            (
                :scope_acceptsWildcard
                AND (
                    tr.authorizations IS NULL
                    OR cardinality(tr.authorizations) = 0
                    OR '*' = ANY(tr.authorizations)
                )
            )
            OR tr.authorizations && CAST(:scope_authorizations AS text[])
        )
        """;

    static TaskRoleSearchPredicate from(Collection<TaskSearchRoleCriteria> criteria) {
        return build(criteria, false);
    }

    static TaskRoleSearchPredicate forPage(Collection<TaskSearchRoleCriteria> criteria) {
        return build(criteria, true);
    }

    private static TaskRoleSearchPredicate build(Collection<TaskSearchRoleCriteria> criteria,
                                                  boolean preserveCorrelation) {
        Map<RoleGroup, Set<String>> groups = groupRoles(criteria);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("taskRoleClassifications", taskClassifications(groups.keySet().stream()
            .map(group -> group.scope().classification()).toList()));

        Map<String, Map<RoleGroup, Set<String>>> permissions = new LinkedHashMap<>();
        groups.forEach((group, names) -> permissions
            .computeIfAbsent(group.scope().permission(), key -> new LinkedHashMap<>()).put(group, names));
        List<String> alternatives = new ArrayList<>();
        permissions.forEach((permission, matches) -> {
            boolean correlatePermission = preserveCorrelation
                || MANAGE_PERMISSION.equals(permission) || OWN_AND_CLAIM_PERMISSION.equals(permission);
            alternatives.add(permissionExists(permission, matches, parameters, correlatePermission));
        });

        return new TaskRoleSearchPredicate(alternatives.isEmpty() ? "FALSE" : String.join(" OR ", alternatives),
            parameters);
    }

    void setParameters(Query query) {
        parameters.forEach(query::setParameter);
    }

    private static Map<RoleGroup, Set<String>> groupRoles(Collection<TaskSearchRoleCriteria> criteria) {
        Map<RoleMatch, Set<String>> authorizationsByRole = new LinkedHashMap<>();
        for (TaskSearchRoleCriteria criterion : criteria) {
            if (canMatch(criterion)) {
                RoleMatch role = new RoleMatch(Scope.from(criterion), criterion.roleName());
                authorizationsByRole.computeIfAbsent(role, key -> new LinkedHashSet<>())
                    .add(criterion.authorizationValue() == null ? WILDCARD : criterion.authorizationValue());
            }
        }

        Map<RoleGroup, Set<String>> groups = new LinkedHashMap<>();
        authorizationsByRole.forEach((role, authorizations) -> {
            Set<String> requiredAuthorizations = role.scope().requiresAuthorization()
                ? Set.copyOf(authorizations) : Set.of();
            RoleGroup group = new RoleGroup(role.scope(), requiredAuthorizations);
            groups.computeIfAbsent(group, key -> new LinkedHashSet<>()).add(role.name());
        });
        return groups;
    }

    private static boolean canMatch(TaskSearchRoleCriteria criterion) {
        return criterion.roleName() != null
            && criterion.classification() != null
            && List.of("U", "P", "R").contains(criterion.classification())
            && criterion.permission() != null
            && PERMISSION_PREDICATES.containsKey(criterion.permission())
            && (OWN_AND_CLAIM_PERMISSION.equals(criterion.permission()) || criterion.caseId() != null
                || criterion.authorizationValue() == null || WILDCARD.equals(criterion.authorizationValue()));
    }

    private static String permissionExists(String permission,
                                           Map<RoleGroup, Set<String>> groups,
                                           Map<String, Object> parameters,
                                           boolean preserveCorrelation) {
        String prefix = "permission_" + permission + "_";
        parameters.put(prefix + "roleNames", groups.values().stream().flatMap(Collection::stream).distinct().toList());
        List<String> scopes = new ArrayList<>();
        List<String> taskScopes = new ArrayList<>();
        groups.forEach((group, names) -> {
            String scopePrefix = "scope_" + permission + "_" + scopes.size() + "_";
            scopes.add(scopePredicate(scopePrefix, group, names, parameters));
            if (preserveCorrelation) {
                taskScopes.add("(" + taskScopePredicate(scopePrefix, group.scope()) + ")");
            }
        });
        // OFFSET 0 keeps permission checks on the task-ID lookup indexes. Pages can stop
        // at their limit; manage/available counts avoid hashing millions of historical roles.
        // Read counts retain the unfenced plan because they have no covering lookup index.
        String permissionSql = PERMISSION_EXISTS.formatted(PERMISSION_PREDICATES.get(permission), prefix,
            String.join(" OR ", scopes), preserveCorrelation ? "OFFSET 0" : "");
        // Expose selective case/location/region scopes to the task scan even though the
        // EXISTS cannot be pulled up. Keep full scope-to-role checks inside it as well.
        return preserveCorrelation
            ? "((" + String.join(" OR ", taskScopes) + ") AND " + permissionSql + ")" : permissionSql;
    }

    private static String taskScopePredicate(String prefix, Scope scope) {
        List<String> constraints = new ArrayList<>();
        addTaskScopeConstraint(constraints, "jurisdiction", prefix + "jurisdiction", scope.jurisdiction());
        addTaskScopeConstraint(constraints, "region", prefix + "region", scope.region());
        addTaskScopeConstraint(constraints, "location", prefix + "location", scope.location());
        addTaskScopeConstraint(constraints, "case_id", prefix + "caseId", scope.caseId());
        return constraints.isEmpty() ? "TRUE" : String.join(" AND ", constraints);
    }

    private static void addTaskScopeConstraint(List<String> constraints,
                                               String column,
                                               String parameter,
                                               String value) {
        if (value != null) {
            constraints.add("t." + column + " = CAST(:" + parameter + " AS text)");
        }
    }

    private static String scopePredicate(String prefix,
                                         RoleGroup group,
                                         Set<String> names,
                                         Map<String, Object> parameters) {
        Scope scope = group.scope();
        parameters.put(prefix + "jurisdiction", scope.jurisdiction());
        parameters.put(prefix + "region", scope.region());
        parameters.put(prefix + "location", scope.location());
        parameters.put(prefix + "caseId", scope.caseId());
        parameters.put(prefix + "classifications", taskClassifications(List.of(scope.classification())));
        parameters.put(prefix + "roleNames", List.copyOf(names));
        String authorizationScope = "";
        if (scope.requiresAuthorization()) {
            parameters.put(prefix + "acceptsWildcard", group.authorizations().contains(WILDCARD));
            parameters.put(prefix + "authorizations", group.authorizations().stream()
                .filter(value -> !WILDCARD.equals(value)).sorted().toArray(String[]::new));
            authorizationScope = AUTHORIZATION_SCOPE;
        }
        // Omit unused authorization columns so generic manage plans can use the covering index.
        return ROLE_SCOPE.formatted(authorizationScope).replace(":scope_", ":" + prefix);
    }

    private static String[] taskClassifications(Collection<String> classifications) {
        if (classifications.contains("R")) {
            return new String[]{"PUBLIC", "PRIVATE", "RESTRICTED"};
        } else if (classifications.contains("P")) {
            return new String[]{"PUBLIC", "PRIVATE"};
        }
        return new String[]{"PUBLIC"};
    }

    private record RoleMatch(Scope scope, String name) {
    }

    private record RoleGroup(Scope scope, Set<String> authorizations) {
    }

    private record Scope(String jurisdiction,
                         String region,
                         String location,
                         String caseId,
                         String permission,
                         String classification) {

        private static Scope from(TaskSearchRoleCriteria criterion) {
            return new Scope(criterion.jurisdiction(), criterion.region(), criterion.location(), criterion.caseId(),
                criterion.permission(), criterion.classification());
        }

        private boolean requiresAuthorization() {
            return OWN_AND_CLAIM_PERMISSION.equals(permission) && caseId == null;
        }
    }
}
