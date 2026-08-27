/*
 * Build B-tree indexes for the no-GIN search path. The search query reads
 * ordinary task columns plus relational permission facts from
 * task_search_permissions.
 *
 * The legacy search_index is deliberately kept so
 * TaskResourceSearchIndexComparisonTest can compare old and new search paths
 * against the same dataset.
 *
 * This script is intended to be applied to a fresh database, so the indexes do
 * not need CONCURRENTLY.
 */

CREATE INDEX search_active_tasks_sort_idx
    ON cft_task_db.tasks USING btree (major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX search_task_filters_idx
    ON cft_task_db.tasks USING btree (
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

CREATE INDEX search_assignee_idx
    ON cft_task_db.tasks USING btree (assignee, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX search_case_id_idx
    ON cft_task_db.tasks USING btree (case_id, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX search_task_type_idx
    ON cft_task_db.tasks USING btree (task_type, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX search_active_tasks_permission_lookup_idx
    ON cft_task_db.tasks USING btree (task_id)
    INCLUDE (jurisdiction, region, location, case_id, security_classification)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;
