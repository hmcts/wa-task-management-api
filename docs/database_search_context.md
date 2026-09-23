# Database Search Context: Direct Task-Role Search

The active indexed-search path is selected by the `wa-task-search-gin-index` LaunchDarkly flag. When enabled, searches use the legacy `search_index` GIN expression index. When disabled, searches use the PostgreSQL-specific, signature-compatible relational design that avoids new GIN indexes. The current LaunchDarkly boolean default is `true`, so the legacy path is used if LaunchDarkly cannot supply a value.

- When enabled, search uses the legacy `search_index` GIN expression index.
- When disabled, search applies task filters to `tasks` and checks permissions
  directly against `task_roles` with B-tree indexes.

## Current Shape

There are two indexed search paths in `TaskResourceCustomRepositoryImpl`, selected by `CFTTaskSearchService`:

* `searchTasksIdsUsingSearchIndex(...)` and `searchTasksCountUsingSearchIndex(...)` use the legacy `search_index` expression GIN path.
* `searchTasksIdsUsingTaskRoles(...)` and `searchTasksCountUsingTaskRoles(...)` use the no-GIN relational path.

The no-GIN path has five parts:

1. restricts `tasks` to indexed, active tasks;
2. applies request filters such as jurisdiction, location, region, work type,
   task type, assignee, case ID, role category, and excluded case IDs;
3. restricts task security classifications to those visible to the requester's
   roles;
4. checks matching role name, permission, scope, and authorization on
   `task_roles`;
5. applies the requested sort and pagination.

The count query uses the same task and permission semantics without ordering,
pagination, or a configured count cap.

Page and count each emit one state predicate: requested states intersected with
`ASSIGNED` and `UNASSIGNED`. Null/empty state requests use both active states;
inactive-only requests return no matches. This avoids overlapping active/requested
state predicates that produced a slow plan in the production clone.

## Permission Semantics

The direct query maps each requested permission to the source columns on
`task_roles`:

| Permission | Required task-role values |
| --- | --- |
| Read (`r`) | `read = true` |
| Manage (`m`) | `manage = true` |
| Own and claim (`a`) | `own = true AND claim = true` |

Role names are grouped only when their task scope and authorization requirements
are identical. This preserves the correlation between a role name and its
jurisdiction, region, location, case, classification, and authorization scope.

Case-scoped roles do not require a task-role authorization match. Organisational
roles used for available-task search accept a wildcard authorization, an empty or
null task-role authorization array, or an overlap with an authorization held by
the user.

Task visibility follows the classification hierarchy:

| Role classification | Visible task classifications |
| --- | --- |
| `U` | `PUBLIC` |
| `P` | `PUBLIC`, `PRIVATE` |
| `R` | `PUBLIC`, `PRIVATE`, `RESTRICTED` |

## Search Indexes

`V1.0.45__replace_search_gin_indexes.sql` adds partial B-tree indexes for active
indexed tasks. These support common page filters and ordering:

- `search_active_tasks_sort_idx`
- `search_task_filters_idx`
- `search_assignee_idx`
- `search_task_type_idx`
- `search_active_tasks_permission_lookup_idx`
- `search_available_tasks_count_idx`
- `search_available_tasks_sort_idx`

`V1.0.46__add_task_role_search_count_indexes.sql` adds indexes specifically for
the direct task-role query:

- `task_roles_search_manage_idx` for manage-permission checks;
- `task_roles_search_available_idx` for own-and-claim checks, including
  `authorizations` to reduce heap reads;
- `search_active_tasks_count_idx` for uncapped active-task counts and their task
  filters.

The legacy `search_index` GIN expression index is retained for the feature-flagged
legacy path. No replacement GIN or GiST indexes and no materialised signature
columns are added.

## Retired Permission Table

The earlier relational implementation copied permissions into
`task_search_permissions` and maintained the table with write-side triggers.
The direct `task_roles` query made that derived table and its two indexes
unnecessary.

`V1.0.47__drop_task_search_permissions.sql` removes the refresh triggers and
functions and drops the table. Dropping the table also removes:

- `task_search_permissions_authorization_lookup_idx`
- `task_search_permissions_task_lookup_idx`

The cleanup is a forward migration so databases that have already applied the
earlier migrations keep valid Flyway checksums.

## Validation

The main validation layers are:

- `TaskResourceCustomRepositoryImplTest` for generated SQL, bound parameters,
  task filters, counts, sorting, and pagination;
- `TaskRoleSearchPredicateTest` for permission grouping, scope correlation,
  classifications, and authorization behavior;
- `TaskResourceRepositoryTest` for Testcontainers-backed task-role page/count
  behavior and the expected database schema;
- `CFTTaskSearchServiceTest` for feature-flag routing and role-assignment
  conversion;
- `CFTTaskSearchServiceComparisonTest` for opt-in local/clone timing diagnostics.
  Its legacy/parity assertions and the older repository comparison class are
  currently commented out; a passing timing replay alone does not prove parity.
