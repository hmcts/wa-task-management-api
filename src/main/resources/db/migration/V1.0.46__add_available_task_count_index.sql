/*
 * Cover the task-side filters and role attributes used by the relational
 * permission count for available tasks. This lets PostgreSQL build the task
 * side of the count without fetching task rows from the heap.
 */
CREATE INDEX search_available_tasks_count_idx
    ON cft_task_db.tasks USING btree (jurisdiction, security_classification, task_id)
    INCLUDE (region, location, case_id, task_type, work_type, role_category)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed AND assignee IS NULL;
