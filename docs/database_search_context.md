# Database Search Context: Materialised Signature Search

This document describes the current task-search implementation in the WA Task Management API.

The active indexed-search path is a PostgreSQL-specific, signature-based design. Exact signatures are stored in materialised `TEXT[]` columns on `cft_task_db.tasks`. Compact hashes are stored in `INTEGER[]` columns and indexed with partial GiST `intarray` indexes. The query uses hashes to find candidates quickly and exact text overlap checks to reject hash collisions.

## 1. Scope and Terminology

There are two task-search implementations behind the main `POST /task` endpoint:

*   **Indexed search**: `CFTTaskDatabaseService.searchForTasks(...)` and `TaskResourceCustomRepositoryImpl`. This is the implementation described in detail below.
*   **Hibernate criteria search**: `CftQueryService.searchForTasks(...)`. This remains available as the fallback implementation.

The legacy LaunchDarkly feature flag name `wa-task-search-gin-index` still selects the indexed implementation for each request. The Java enum constant remains `WA_TASK_SEARCH_GIN_INDEX`.

The legacy GIN index named `search_index` was removed by `V1.0.44__replace_search_gin_indexes.sql`. The `tasks.indexed` boolean remains the index-membership control flag. Setting `indexed = false` does not delete a task. It clears the materialised search arrays and excludes the task from indexed-search SQL.

## 2. Current-State Summary

The indexed search has four cooperating parts:

1.  PostgreSQL expands each searchable task into materialised arrays of exact filter and RBAC signatures.
2.  Java expands the request filters and the user's role assignments into request-side signatures.
3.  PostgreSQL hashes the request-side signatures and uses partial GiST indexes to find candidates.
4.  PostgreSQL tests exact `TEXT[]` overlap before returning rows, preventing hash-collision false positives.

The active indexes are created by `V1.0.44__replace_search_gin_indexes.sql`:

```sql
CREATE INDEX search_filter_signature_hashes_idx
    ON cft_task_db.tasks USING gist (filter_signature_hashes gist__intbig_ops)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX search_role_signature_hashes_idx
    ON cft_task_db.tasks USING gist (role_signature_hashes gist__intbig_ops)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;
```

Important current-state facts:

*   The legacy `search_index` GIN expression index no longer exists.
*   Exact signatures and their hashes are persisted on `tasks`.
*   GiST hash indexes include only `ASSIGNED` and `UNASSIGNED` tasks with `indexed = true`.
*   The indexed query also includes `WHERE indexed`, so an unindexed task is excluded even when PostgreSQL does not use the index for a particular plan.
*   A `task_roles` trigger refreshes parent RBAC search columns in the same transaction.

## 3. Relevant Database Model

### 3.1 `tasks`

`cft_task_db.tasks` is the central task table. The indexed-search expressions use:

| Column | Search role |
| --- | --- |
| `task_id` | Function input and returned identifier |
| `state` | Filter signature input, partial-index predicate, and explicit SQL constraint |
| `jurisdiction` | Filter-signature and role-signature input |
| `role_category` | Filter-signature input |
| `work_type` | Filter-signature input |
| `region` | Filter-signature and role-signature input |
| `location` | Filter-signature and role-signature input |
| `case_id` | Role-signature input and optional SQL include/exclude constraint |
| `security_classification` | Role-signature input |
| `assignee` | Optional SQL constraint with a partial B-tree index |
| `task_type` | Optional SQL constraint outside the signature indexes |
| `indexed` | Partial-index membership flag and mandatory indexed-query predicate |
| `filter_signatures` | Materialised exact task-filter signatures |
| `role_signatures` | Materialised exact RBAC signatures |
| `filter_signature_hashes` | Compact filter-signature candidates indexed by GiST |
| `role_signature_hashes` | Compact RBAC-signature candidates indexed by GiST |

The `indexed` column was added by `V1.0.25__add_performance_indexing.sql` as:

```sql
alter table cft_task_db.tasks add column indexed boolean not null default false;
```

New task rows therefore begin outside indexed search unless application code explicitly sets the flag to `true`.

### 3.2 `task_roles`

`cft_task_db.task_roles` stores the permissions configured for each task. The full table includes granular permissions used elsewhere in the API. The indexed search consumes only:

| Column | Indexed-search role |
| --- | --- |
| `task_id` | Links permissions to a task |
| `role_name` | Included in each RBAC signature |
| `read` | Produces permission code `r` |
| `manage` | Produces permission code `m` |
| `own` and `claim` | Together produce available-task permission code `a` |
| `authorizations` | Expanded for available-task permission code `a` |

Other granular permissions such as `execute`, `complete`, `assign`, and `unassign` are not represented in indexed search. They remain relevant to action authorization after a task has been found.

### 3.3 `task_permissions` view

`V1.0.25__add_performance_indexing.sql` recreates `cft_task_db.task_permissions` as a normalized view over `task_roles`. It converts configured booleans into the three permission codes needed by indexed search:

| Task-role condition | Emitted permission | Authorization behavior |
| --- | --- | --- |
| `read = true` | `r` | Always `*` |
| `manage = true` | `m` | Always `*` |
| `own = true and claim = true` | `a` | One row per configured authorization, or `*` when none are configured |

The `a` code means "available to own and claim". It is not a physical `task_roles` column.

### 3.4 `classifications` view

The static `cft_task_db.classifications` view defines which role-assignment classifications may see each task classification:

| Task classification (`lower`) | Matching role-assignment classifications (`higher`) |
| --- | --- |
| `PUBLIC` | `PUBLIC`, `PRIVATE`, `RESTRICTED` |
| `PRIVATE` | `PRIVATE`, `RESTRICTED` |
| `RESTRICTED` | `RESTRICTED` |

The database-side role-signature function abbreviates the matching `higher` values as `U`, `P`, and `R`.

## 4. Database-Side Signature Generation

### 4.1 Common wildcard convention

The helper function `cft_task_db.add_wildcard(text)` returns:

*   `['*']` when its argument is `null`.
*   `['*', value]` when its argument is non-null.

Database-side signature functions use Cartesian products of these arrays. This pre-generates the wildcard combinations that can match requests with omitted filters or role-assignment attributes.

The literal `*` is embedded inside colon-delimited text signatures. It is not SQL wildcard syntax.

### 4.2 Abbreviations

The database uses compact values inside signatures:

| Domain | Full value | Signature value |
| --- | --- | --- |
| Task state | `ASSIGNED` | `A` |
| Task state | `UNASSIGNED` | `U` |
| Role category | `JUDICIAL` | `J` |
| Role category | `LEGAL_OPERATIONS` | `L` |
| Role category | `ADMIN` | `A` |
| Role category | `CTSC` | `C` |
| Role category | `ENFORCEMENT` | `E` |
| Security classification | `PUBLIC` | `U` |
| Security classification | `PRIVATE` | `P` |
| Security classification | `RESTRICTED` | `R` |

`ENFORCEMENT -> E` was added later by `V1.0.41__add_enforcement_role_category_abbreviation.sql`.

### 4.3 Filter signatures

The active database function is:

```sql
cft_task_db.filter_signatures(
    task_id,
    state,
    jurisdiction,
    role_category,
    work_type,
    region,
    location
) returns text[]
```

Its signature format is:

```text
state:jurisdiction:role_category:work_type:region:location
```

For each field, the task contributes its value and a wildcard when the value is non-null. The state and role category are abbreviated before expansion.

For an assigned IA judicial task with `work_type = hearing_work`, `region = 1`, and `location = 765324`, examples include:

```text
A:IA:J:hearing_work:1:765324
*:IA:J:hearing_work:1:765324
A:*:J:*:1:765324
*:*:*:*:*:*
```

With six non-null inputs, a task produces up to `2^6 = 64` filter signatures.

The `task_id` parameter is retained in the active function signature but is not used by its body.

### 4.4 Role signatures

The active database function is:

```sql
cft_task_db.role_signatures(
    task_id,
    jurisdiction,
    region,
    location,
    case_id,
    security_classification
) returns text[]
```

Its signature format is:

```text
jurisdiction:region:location:role_name:case_id:permission:classification:authorization
```

The function:

1.  Expands task `jurisdiction`, `region`, and `location` into exact-or-wildcard combinations.
2.  Reads normalized `r`, `m`, and `a` permissions from `task_permissions`.
3.  Expands the task classification into the role-assignment classifications that are allowed to see it.
4.  Emits organizational-role signatures with wildcard case ID `*`.
5.  Emits case-role signatures with the task's exact `case_id`.

Example organizational signature:

```text
IA:1:765324:tribunal-caseworker:*:r:U:*
```

Example case-role signature:

```text
IA:1:765324:case-manager:1623278362431003:r:U:*
```

Example available-task signature requiring an authorization:

```text
IA:1:765324:tribunal-caseworker:*:a:U:skill2
```

For organizational signatures, authorizations come from `task_permissions`. For case-role signatures, the function replaces authorization with `*`.

The legacy signature function can emit duplicate values because its `case_ids` expansion is redundant. `V1.0.43__materialise_search_signatures.sql` canonicalises materialised arrays with `DISTINCT`, keeping storage and GiST inputs compact without changing exact-match semantics.

## 5. Java-Side Signature Generation

The application builds request-side signatures before executing SQL. The request-side strings must remain format-compatible with the database-side strings.

### 5.1 Filter signatures from search criteria

`SearchFilterSignatureBuilder.buildFilterSignatures(...)` builds:

```text
state:jurisdiction:role_category:work_type:region:location
```

For each request field:

*   A missing or empty collection contributes `*`.
*   A populated collection contributes its requested values.
*   Multiple populated collections form a Cartesian product.
*   Duplicate results are removed by collecting into a `HashSet`.

Unlike the database side, request-side expansion does not add both `*` and each requested value. A request field is either constrained to supplied values or represented by a single wildcard.

For example, a request for assigned or unassigned IA tasks at location `765324`, with no other filter, becomes:

```text
A:IA:*:*:*:765324
U:IA:*:*:*:765324
```

### 5.2 Role signatures from role assignments

`RoleSignatureBuilder.buildRoleSignatures(...)` converts the user's role assignments into:

```text
jurisdiction:region:base_location:role_name:case_id:permission:classification:authorization
```

The builder:

*   Keeps only `STANDARD`, `SPECIFIC`, and `CHALLENGED` grants for positive matching.
*   Removes role assignments that cannot match explicitly requested jurisdiction, region, location, or case ID filters.
*   Uses `*` when a role-assignment attribute is absent.
*   Uses the role assignment's abbreviated classification.
*   Chooses exactly one permission code from the request context.
*   Collects signatures into a `HashSet`.

Permission selection is:

| Request context | Required signature permission |
| --- | --- |
| General search / no special context | `r` |
| `ALL_WORK` | `m` |
| `AVAILABLE_TASKS` | `a` |

Authorization handling is:

| Search and role type | Request-side authorization values |
| --- | --- |
| `AVAILABLE_TASKS` with organizational role | All role-assignment authorizations plus `*` |
| Any other search | `*` only |
| Case role in any search | `*` only |

### 5.3 Excluded grants

`CFTTaskDatabaseService.buildExcludedCaseIds(...)` handles `EXCLUDED` grants separately. It extracts non-null `caseId` attributes and passes them into SQL as negative case constraints.

`EXCLUDED` grants do not become positive role signatures.

## 6. Indexed Query Shape

`TaskResourceCustomRepositoryImpl` builds both the ID query and count query from this base:

```sql
FROM cft_task_db.tasks t
WHERE indexed
AND t.filter_signature_hashes
    && cft_task_db.signature_hashes(CAST(:filterSignature AS text[]))
AND t.role_signature_hashes
    && cft_task_db.signature_hashes(CAST(:roleSignature AS text[]))
AND t.filter_signatures && CAST(:filterSignature AS text[])
AND t.role_signatures && CAST(:roleSignature AS text[])
```

The PostgreSQL `&&` operator is true when arrays overlap. Hash overlap is an indexed candidate check. Exact text overlap remains mandatory, so a 32-bit hash collision cannot grant access to a task.

### 6.1 Additional SQL constraints

The repository adds ordinary SQL predicates after the signature predicates:

| Search input | Added SQL |
| --- | --- |
| `AVAILABLE_TASKS` | `AND assignee IS NULL` |
| One requested user | `AND assignee = :assignee` |
| Multiple requested users | `AND assignee IN (:assignee)` |
| No requested states | `AND state IN ('ASSIGNED', 'UNASSIGNED')` |
| Requested states | `AND state IN (...)` |
| One requested case ID | `AND case_id = :caseId` |
| Multiple requested case IDs | `AND case_id IN (:caseId)` |
| One excluded case ID | `AND case_id <> :excludedCaseId` |
| Multiple excluded case IDs | `AND case_id NOT IN (:excludedCaseId)` |
| One requested task type | `AND task_type = :taskType` |
| Multiple requested task types | `AND task_type IN (:taskType)` |

`assignee` has a partial B-tree index for active indexed tasks. `case_id` uses the existing `idx_tasks_case_id` B-tree index. `state` is part of both the filter signature and the partial-index predicates. `task_type` is filtered outside the signature indexes.

### 6.2 ID query, count query, and entity fetch

The indexed search is a multi-step read:

1.  Generate Java-side filter signatures, role signatures, and excluded case IDs.
2.  Run the native ID query with sort order, `OFFSET`, and `LIMIT`.
3.  Return an empty result with total `0` immediately when no task IDs match.
4.  Run a second native query with the same predicates and `count(*)`.
5.  Load full `TaskResource` entities using `findAllByTaskIdIn(taskIds, sort)`.
6.  Map entities into API tasks and calculate the user's returned permission union.

The native ID query and the entity fetch both apply the same requested sort followed by deterministic defaults:

```text
major_priority ASC,
priority_date ASC,
minor_priority ASC,
task_id ASC
```

### 6.3 Permission calculation after search

The indexed predicates determine whether a task is visible in the requested context. After loading each entity, `CFTTaskMapper.mapToTaskAndExtractPermissionsUnion(...)` calculates the permissions returned in the API resource.

That post-search permission union uses the task's `TaskRoleResource` records and the user's role assignments. This is distinct from the three coarse search permission codes `r`, `m`, and `a`.

## 7. Non-GIN Index Structure

### 7.1 Why `intarray` is installed

`V1.0.43__materialise_search_signatures.sql` installs:

```sql
CREATE EXTENSION IF NOT EXISTS intarray;
```

The `intarray` extension provides GiST operator classes for `INTEGER[]` overlap. The migration stores canonical `hashtext(...)` results for each exact signature and indexes those compact arrays with `gist__intbig_ops`. GiST may return false-positive candidates; the exact `TEXT[]` predicates remove them.

### 7.2 Partial-index boundary

The index predicate is:

```sql
where state in ('ASSIGNED','UNASSIGNED') and indexed
```

Completed, cancelled, terminated, and other non-active states are intentionally absent from the partial GiST and assignee indexes.

The indexed-search repository can technically append requested non-active states to SQL. Materialised arrays are retained for indexed non-active rows, preserving existing sequential-scan behavior, but the performant indexed path is designed for assigned and unassigned work.

### 7.3 Relevant supporting indexes

The primary schema also contains supporting B-tree indexes, including:

*   `idx_fk_task_id` on `task_roles(task_id)`, relevant when `role_signatures(...)` reads `task_permissions`.
*   `idx_read`, `idx_manage`, and `idx_own_claim` on `task_roles`, relevant to view filtering.
*   `idx_tasks_case_id` on `tasks(case_id)`.
*   `search_assignee_idx` on active indexed `tasks(assignee)`.

Earlier task-table B-tree indexes such as `idx_region_state`, `idx_task_case_id`, and `idx_tasks_role_category` were removed by `V1.0.24__drop_task_table_index.sql` before `search_index` was introduced.

## 8. Index Maintenance and Consistency Protocol

### 8.1 Why maintenance needs a protocol

`role_signatures(...)` reads `cft_task_db.task_permissions`, which reads `task_roles`. `V1.0.43__materialise_search_signatures.sql` changes its volatility from `IMMUTABLE` to `STABLE` because it is no longer used in an expression index.

Two triggers maintain the columns:

*   `refresh_task_search_columns_on_tasks` recomputes or clears all materialised search columns when relevant task attributes or `indexed` change.
*   `refresh_task_search_columns_on_task_roles` recomputes the parent task's RBAC signatures after relevant role inserts, deletes, and updates.

The `task_roles` trigger runs in the same transaction as the permission change. It handles `task_id`, `role_name`, `read`, `manage`, `own`, `claim`, and `authorizations`. Changes to unrelated granular action permissions do not churn the search indexes.

The existing application protocol remains useful for bulk changes:

1.  Set `tasks.indexed = false`, clearing search columns and removing GiST entries.
2.  Modify the task and its permissions.
3.  Set `tasks.indexed = true`, materialising final signatures once.

### 8.2 Task initiation

Task initiation follows the protocol indirectly:

1.  New rows default to `indexed = false`.
2.  Initiation configures the task and its DMN-derived `task_roles`.
3.  `ExclusiveTaskActionsController.initiate(...)` calls `TaskManagementService.updateTaskIndex(taskId)` after initiation.
4.  `updateTaskIndex(...)` obtains a pessimistic write lock, sets `indexed = true`, and saves the task in a new transaction.

The separate final update materialises search columns and adds the configured task to the partial indexes.

### 8.3 Task reconfiguration

Bulk reconfiguration explicitly removes tasks from indexed search before recalculating task attributes and permissions:

1.  `MarkTaskReconfigurationService` finds active tasks for requested case IDs.
2.  It sets `reconfigureRequestTime` and `indexed = false`.
3.  Later, `TaskReconfigurationTransactionHandler` applies reconfiguration and DMN-derived roles.
4.  `resetIndexed(...)` sets `indexed = true` again for `ASSIGNED` or `UNASSIGNED` tasks.
5.  Saving the task materialises final signatures and creates fresh GiST index entries.

While a task is marked but not yet successfully reconfigured, indexed search intentionally excludes it.

### 8.4 Operational repair path

The exclusive task-operation endpoint supports `update_search_index`.

`UpdateSearchIndexService`:

1.  Loads `ASSIGNED` and `UNASSIGNED` tasks where `indexed = false`.
2.  Obtains a write lock for each task.
3.  Sets `indexed = true`.
4.  Saves each task.

This is an application-level repair path for active tasks left unindexed.

### 8.5 Full rebuild procedure

The database procedure `cft_task_db.reindex_all_tasks()` performs a bulk rebuild:

```sql
update cft_task_db.tasks set indexed = false;
commit;
update cft_task_db.tasks
set indexed = true
where state in (
    'ASSIGNED'::cft_task_db.task_state_enum,
    'UNASSIGNED'::cft_task_db.task_state_enum
);
commit;
```

The repository includes `src/main/resources/scripts/reindex_all_tasks_search_data.sql` as a one-line invocation:

```sql
call cft_task_db.reindex_all_tasks();
```

This procedure deliberately leaves non-active tasks unindexed.

### 8.6 Consistency risks

The database now enforces search-column refresh for the supported write paths:

*   Relevant `tasks` changes refresh filter and RBAC signatures.
*   Relevant `task_roles` changes refresh RBAC signatures on the parent task in the same transaction.
*   Hash arrays are derived whenever exact arrays are refreshed.
*   A failed reconfiguration can leave a task intentionally absent from indexed search until retry or repair.

Direct manual updates to materialised columns remain an operational hazard and should not be used.

## 9. Migration History

### `V1.0.15__add_tasks_table_index.sql`

Added general task and task-role B-tree indexes used by the earlier search implementation.

### `V1.0.23__add_tasks_table_granular_permission_index.sql`

Added `idx_manage` and `idx_own_claim` on `task_roles`.

### `V1.0.24__drop_task_table_index.sql`

Dropped several earlier task-table B-tree indexes before the signature-based GIN index was introduced.

### `V1.0.25__add_performance_indexing.sql`

Introduced:

*   `tasks.indexed`.
*   The `btree_gin` extension.
*   `classifications`.
*   The current shape of `task_permissions`.
*   Wildcard and abbreviation helpers.
*   Initial one-argument versions of `filter_signatures(task_id)` and `role_signatures(task_id)`.
*   Initial `search_index`.
*   `reindex_all_tasks()`.

The initial signature functions queried `tasks` internally by `task_id`.

### `V1.0.26__update_performance_index_function.sql`

Replaced the initial expression index with the later GIN form. Task scalar values are passed directly into the functions:

*   `filter_signatures(task_id, state, jurisdiction, role_category, work_type, region, location)`
*   `role_signatures(task_id, jurisdiction, region, location, case_id, security_classification)`

`role_signatures(...)` still reads `task_permissions` by `task_id`.

### `V1.0.28__drop_old_signature_functions.sql`

Dropped the obsolete one-argument function overloads.

### `V1.0.41__add_enforcement_role_category_abbreviation.sql`

Extended role-category abbreviation support with `ENFORCEMENT -> E`.

### `V1.0.43__materialise_search_signatures.sql`

Introduced:

*   Exact materialised `filter_signatures` and `role_signatures` arrays on `tasks`.
*   Compact `filter_signature_hashes` and `role_signature_hashes` arrays on `tasks`.
*   Canonicalisation to remove duplicate stored signatures.
*   Refresh triggers on `tasks` and relevant `task_roles` changes.
*   Exact text predicates after hash candidate lookup to reject collisions.

### `V1.0.44__replace_search_gin_indexes.sql`

Builds partial GiST `intarray` indexes and a partial B-tree `assignee` index with `CREATE INDEX CONCURRENTLY`, then drops the legacy `search_index` GIN expression index with `DROP INDEX CONCURRENTLY`. Keeping GIN until the replacement indexes exist avoids a rollout performance gap.

## 10. Feature Flag and Endpoint Routing

`TaskSearchController.searchWithCriteria(...)`:

1.  Retrieves the caller's access-control response.
2.  Returns an empty successful response when the caller has no role assignments.
3.  Reads `wa-task-search-gin-index`.
4.  Maps the API request to `SearchRequest`.
5.  Calls indexed search when the flag is enabled.
6.  Calls Hibernate criteria search when the flag is disabled.

The non-production `POST /task/extended-search` testing endpoint always calls the Hibernate criteria implementation.

`POST /task/search-for-completable` is a separate workflow and is not implemented by indexed search.

## 11. Primary and Replica Boundary

The indexed-search implementation belongs to the primary `cft_task_db` schema.

The replica migrations create a reporting-oriented `tasks` table that includes an `indexed` column, but the replica schema does not define:

*   Materialised signature and hash columns.
*   GiST search indexes.
*   Signature functions.
*   `task_permissions`.
*   `task_roles`.

Logical replication publication setup in `TaskResourceRepository` publishes `tasks` and `work_types`, not `task_roles`. The reporting replica still cannot execute the RBAC indexed search without additional schema work.

## 12. Test Coverage That Documents Current Behavior

### Signature builders

*   `SearchFilterSignatureBuilderTest` documents wildcard defaults, abbreviations, and Cartesian-product growth across filter dimensions.
*   `RoleSignatureBuilderTest` documents request-context permission codes, supported grant types, role-attribute filtering, case roles, organizational roles, classification abbreviations, and available-task authorizations.

### Native repository query

*   `TaskResourceCustomRepositoryImplTest` asserts the generated native SQL, parameters, extra constraints, pagination, and count-query shape.
*   `TaskResourceRepositoryTest` runs integration coverage for basic matching, case ID, task type, assignee, state, filter-signature overlap, role-signature overlap, available tasks, default active states, excluded case IDs, materialisation, collision rejection, `task_roles` refresh, and GiST index definitions.

### Service orchestration

*   `CFTTaskDatabaseServiceSearchTest` exercises indexed search end to end with PostgreSQL-backed fixtures, including sorting, paging, task counts, case role assignments, authorizations, `AVAILABLE_TASKS`, `ALL_WORK`, assignees, task types, and exclusions.
*   `TaskSearchControllerTest` verifies that the LaunchDarkly flag selects indexed search or Hibernate criteria search.
*   Reconfiguration and update-index tests verify the `indexed` lifecycle.

The tests validate behavior and GiST schema definitions. Production-scale latency, query plans, storage, and write-amplification measurements still need representative data volumes.

## 13. Operational Considerations

The materialised design preserves the existing RBAC and filter semantics while removing GIN. Operational validation should continue to cover:

1.  Preserve RBAC semantics for general search (`r`), all work (`m`), and available work (`a`).
2.  Preserve classification ordering: restricted role assignments can see lower-classification tasks, not the reverse.
3.  Preserve organizational-role and case-role semantics.
4.  Preserve available-task authorization matching and `assignee IS NULL`.
5.  Preserve `EXCLUDED` case-ID handling.
6.  Preserve wildcard behavior for missing search filters and missing role-assignment attributes.
7.  Preserve filtering for jurisdiction, region, location, role category, work type, state, case ID, user, and task type.
8.  Define active-state behavior for `ASSIGNED` and `UNASSIGNED`, and explicitly decide what happens for other states.
9.  Keep task and `task_roles` mutations transactionally visible through the refresh triggers.
10. Retain repair and full-rebuild procedure checks.
11. Measure read latency, write latency, storage, index growth, and operational cost with realistic role and permission cardinalities before production rollout.
12. Decide when to rename or retire the legacy `wa-task-search-gin-index` rollout flag.
13. Account for primary-versus-replica data availability if indexed search is ever moved outside the primary schema.

## 14. Source Map

The main implementation files are:

| Concern | Source |
| --- | --- |
| Legacy GIN index and SQL functions | `src/main/resources/db/migration/V1.0.25__add_performance_indexing.sql` and `V1.0.26__update_performance_index_function.sql` |
| Materialised signatures and refresh triggers | `src/main/resources/db/migration/V1.0.43__materialise_search_signatures.sql` |
| Concurrent GiST index replacement | `src/main/resources/db/migration/V1.0.44__replace_search_gin_indexes.sql` |
| Obsolete overload removal | `src/main/resources/db/migration/V1.0.28__drop_old_signature_functions.sql` |
| Enforcement role category | `src/main/resources/db/migration/V1.0.41__add_enforcement_role_category_abbreviation.sql` |
| Native indexed query | `src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/repository/TaskResourceCustomRepositoryImpl.java` |
| Indexed-search orchestration | `src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/services/CFTTaskDatabaseService.java` |
| Request filter signatures | `src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/services/signature/SearchFilterSignatureBuilder.java` |
| Role-assignment signatures | `src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/services/signature/RoleSignatureBuilder.java` |
| Feature-flag routing | `src/main/java/uk/gov/hmcts/reform/wataskmanagementapi/controllers/TaskSearchController.java` |
| Reconfiguration removal and reinsertion | `MarkTaskReconfigurationService.java` and `TaskReconfigurationTransactionHandler.java` |
| Repair operation | `UpdateSearchIndexService.java` |
| Full rebuild invocation | `src/main/resources/scripts/reindex_all_tasks_search_data.sql` |
