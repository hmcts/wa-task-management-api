# Database Search Context: Relational Permission Search

This document describes the current indexed task-search implementation in the WA Task Management API.

The active indexed-search path is a PostgreSQL-specific, signature-compatible design that avoids new GIN indexes. The repository still receives request-side filter and role signatures. Java parses filter signatures into concrete task-column predicates, while PostgreSQL parses role signatures and matches them against ordinary task columns plus a small relational permission table.

The legacy `search_index` GIN expression index is intentionally retained during the transition so `TaskResourceSearchIndexComparisonTest` can compare the old and new search paths against the same dataset. A follow-up migration should drop it only after accuracy and production-scale timings are accepted.

## Current Shape

There are two indexed search paths in `TaskResourceCustomRepositoryImpl`:

* `searchTasksIdsOld(...)` and `searchTasksCountOld(...)` use the legacy `search_index` expression GIN path.
* `searchTasksIds(...)` and `searchTasksCount(...)` use the no-GIN relational path.

The no-GIN path has four parts:

1. Java still builds request-side filter signatures and role signatures.
2. Java converts filter-signature abbreviations into B-tree-friendly task predicates.
3. SQL applies task filters directly against `tasks`.
4. SQL parses role signatures and checks RBAC with `EXISTS` against `task_search_permissions`.

No materialised signature arrays are stored on `tasks`, and no replacement GIN or GiST index is created.

## Search Tables

### `tasks`

The search query reads these task columns directly:

| Column | Role |
| --- | --- |
| `task_id` | Returned identifier and permission-table join key |
| `indexed` | Mandatory inclusion flag |
| `state` | Filter-signature match, explicit state filter, active partial-index predicate |
| `jurisdiction` | Filter and role assignment match |
| `role_category` | Filter match |
| `work_type` | Filter match |
| `region` | Filter and role assignment match |
| `location` | Filter and role assignment match |
| `case_id` | Case filter, excluded-case filter, case-role match |
| `task_type` | Task-type filter |
| `assignee` | User filter and available-task exclusion |
| `security_classification` | Classification visibility check |
| `major_priority`, `priority_date`, `minor_priority` | Default task ordering |

### `task_search_permissions`

`V1.0.43__create_task_search_permissions.sql` creates:

```sql
CREATE TABLE cft_task_db.task_search_permissions
(
    task_id             TEXT NOT NULL,
    role_name           TEXT NOT NULL,
    permission          TEXT NOT NULL,
    authorization_value TEXT NOT NULL,
    PRIMARY KEY (task_id, role_name, permission, authorization_value),
    FOREIGN KEY (task_id) REFERENCES cft_task_db.tasks (task_id) ON DELETE CASCADE,
    CHECK (permission IN ('r', 'm', 'a'))
);
```

This table is derived from the existing `task_permissions` view:

* `r` means read permission.
* `m` means manage permission for all-work searches.
* `a` means own-and-claim permission for available-task searches.
* `authorization_value` is `*` for read/manage and either `*` or a configured authorization for available tasks.

The table stores only actual permission facts. It does not store wildcard-expanded task signatures.

## Refresh Rules

`refresh_task_search_permissions(task_id)` deletes and reinserts permission facts for one task.

Refresh triggers call it when:

* `tasks.indexed` changes, including clearing rows when a task is no longer indexed.
* a relevant `task_roles` field changes: `task_id`, `role_name`, `read`, `manage`, `own`, `claim`, or `authorizations`.
* a task is deleted, via `ON DELETE CASCADE`.

The initial migration backfills rows for all tasks already marked `indexed = true`.

## Query Semantics

### Filter signatures

The request filter signature format remains:

```text
state:jurisdiction:role_category:work_type:region:location
```

Java parses each value. `*` becomes "no constraint" for that dimension. State abbreviations are expanded before binding (`U -> UNASSIGNED`, `A -> ASSIGNED`), and role-category abbreviations are expanded before binding (`J`, `L`, `A`, `C`, `E`).

A task matches when at least one parsed filter signature matches all constrained dimensions. For one fully-constrained signature this becomes ordinary equality predicates:

```sql
AND (
    t.state = CAST(:filterState0 AS cft_task_db.task_state_enum)
    AND t.jurisdiction = :filterJurisdiction0
    AND t.role_category = :filterRoleCategory0
    AND t.work_type = :filterWorkType0
    AND t.region = :filterRegion0
    AND t.location = :filterLocation0
)
```

Multiple filter signatures are joined with `OR`. Wildcard dimensions are omitted from that signature's predicate.

The repository also adds direct SQL predicates for fields present on `SearchRequest`, such as `case_id`, `task_type`, `assignee`, `jurisdiction`, `location`, `region`, and `work_type`.

### Role signatures

The request role signature format remains:

```text
jurisdiction:region:location:role_name:case_id:permission:classification:authorization
```

The authorization tail may itself contain colons, so SQL parses the first seven fields with `string_to_array(...)` and keeps the remaining tail with `regexp_replace(...)`.

A task matches RBAC when at least one parsed role row matches a permission fact and the task attributes:

```sql
EXISTS (
    SELECT 1
    FROM task_search_permissions tsp
    JOIN request_role_signatures rs
      ON rs.role_name = tsp.role_name
     AND rs.permission = tsp.permission
    WHERE tsp.task_id = t.task_id
      AND (rs.jurisdiction IS NULL OR rs.jurisdiction = t.jurisdiction)
      AND (rs.region IS NULL OR rs.region = t.region)
      AND (rs.location IS NULL OR rs.location = t.location)
      AND (rs.case_id IS NULL OR rs.case_id = t.case_id)
      AND (rs.case_id IS NOT NULL OR tsp.authorization_value = rs.authorization_value)
      AND classification_matches_task(rs.classification, t.security_classification)
)
```

The case-role rule is deliberate:

* A request role signature with a case ID represents a case role and ignores task permission authorizations, matching the legacy case-role signature behavior.
* A request role signature without a case ID represents an organisational role and must match `authorization_value`.

Classification is equivalent to the legacy `classifications` view:

| Task classification | Matching role classifications |
| --- | --- |
| `PUBLIC` | `U`, `P`, `R` |
| `PRIVATE` | `P`, `R` |
| `RESTRICTED` | `R` |

## Indexes

`V1.0.43__create_task_search_permissions.sql` creates a B-tree lookup index:

```sql
CREATE INDEX task_search_permissions_lookup_idx
    ON cft_task_db.task_search_permissions (permission, role_name, task_id, authorization_value);
```

The table primary key also supports task-first permission checks:

```sql
PRIMARY KEY (task_id, role_name, permission, authorization_value)
```

`V1.0.44__replace_search_gin_indexes.sql` creates partial B-tree indexes on active indexed tasks:

```sql
CREATE INDEX CONCURRENTLY search_active_tasks_sort_idx
    ON cft_task_db.tasks (major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_filter_signature_idx
    ON cft_task_db.tasks (
        state,
        jurisdiction,
        role_category,
        work_type,
        region,
        location,
        major_priority,
        priority_date,
        minor_priority,
        task_id
    )
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_assignee_idx
    ON cft_task_db.tasks (assignee, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_case_id_idx
    ON cft_task_db.tasks (case_id, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_task_type_idx
    ON cft_task_db.tasks (task_type, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;
```

These indexes are intentionally much simpler than the legacy `search_index` GIN index. They do not index exploded arrays, and they should not require GIN reindex maintenance.

## Migration Notes

### `V1.0.25__add_performance_indexing.sql`

Introduced:

* `tasks.indexed`
* `task_permissions`
* legacy signature functions
* legacy `search_index` GIN

### `V1.0.26__update_performance_index_function.sql`

Updated the legacy signature functions and recreated `search_index`.

### `V1.0.43__create_task_search_permissions.sql`

Adds the no-GIN derived permission table, refresh functions, refresh triggers, and backfill.

### `V1.0.44__replace_search_gin_indexes.sql`

Adds B-tree indexes for the no-GIN query path. It deliberately keeps the legacy `search_index` for comparison.

The fresh migration path does not add task-level materialised signature columns such as `filter_signatures`, `role_signatures`, `filter_signature_hashes`, or `role_signature_hashes`.

## Validation

The important validation layers are:

* `TaskResourceCustomRepositoryImplTest`: generated SQL, parameters, counts, pagination, and old/new query split.
* `TaskResourceRepositoryTest`: table refresh behavior, role-change behavior, no-GIN schema checks, and functional search results.
* `TaskResourceSearchIndexComparisonTest`: local production-like comparison of legacy `search_index` versus the no-GIN relational query for task IDs, counts, and timings.

Production-scale validation still needs representative data volumes. The acceptance bar is exact result parity with `search_index`, materially smaller indexes, and p95/p99 latency that avoids the timeout pattern caused by GIN bloat.

On the current local comparison dataset, the no-GIN path matched legacy task IDs and full-page counts across 500 scenarios. The new indexes were materially smaller than the legacy GIN index: `search_filter_signature_idx` was about 114 MB and the permission-table indexes were about 696-697 MB each, versus about 20 GB for `search_index`.
