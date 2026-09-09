
CREATE INDEX CONCURRENTLY IF NOT EXISTS search_active_tasks_sort_idx
    ON cft_task_db.tasks USING btree (major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_task_filters_idx
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

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_assignee_idx
    ON cft_task_db.tasks USING btree (assignee, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_case_id_idx
    ON cft_task_db.tasks USING btree (case_id, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_task_type_idx
    ON cft_task_db.tasks USING btree (task_type, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_active_tasks_permission_lookup_idx
    ON cft_task_db.tasks USING btree (task_id)
    INCLUDE (jurisdiction, region, location, case_id, security_classification)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY IF NOT EXISTS search_available_tasks_count_idx
  ON cft_task_db.tasks USING btree (jurisdiction, security_classification, task_id)
  INCLUDE (region, location, case_id, task_type, work_type, role_category)
  WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed AND assignee IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS task_search_permissions_authorization_lookup_idx
  ON cft_task_db.task_search_permissions USING btree
    (permission, role_name, authorization_value, task_id)
  WHERE authorization_value IS NOT NULL;

CREATE  INDEX CONCURRENTLY IF NOT EXISTS task_search_permissions_null_auth_idx
  ON cft_task_db.task_search_permissions (task_id, role_name, permission)
  WHERE authorization_value IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS task_search_permissions_auth_idx
  ON cft_task_db.task_search_permissions (task_id, role_name, permission, authorization_value)
  WHERE authorization_value IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS task_search_permissions_lookup_idx
  ON cft_task_db.task_search_permissions (permission, role_name, task_id, authorization_value);
