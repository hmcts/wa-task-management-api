CREATE INDEX CONCURRENTLY search_active_tasks_sort_idx
    ON cft_task_db.tasks USING btree (major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_task_filters_idx
    ON cft_task_db.tasks USING btree (
        state,
        jurisdiction,
        work_type,
        role_category,
        location,
        region,
        major_priority,
        priority_date,
        minor_priority,
        task_id
    )
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_assignee_idx
    ON cft_task_db.tasks USING btree (assignee, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed AND assignee IS NOT NULL;

CREATE INDEX CONCURRENTLY search_task_type_idx
    ON cft_task_db.tasks USING btree (task_type, major_priority, priority_date, minor_priority, task_id)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_active_tasks_permission_lookup_idx
    ON cft_task_db.tasks USING btree (task_id)
    INCLUDE (jurisdiction, region, location, case_id, security_classification)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_available_tasks_count_idx
  ON cft_task_db.tasks USING btree (jurisdiction, work_type)
  INCLUDE (task_id, security_classification, region, location, case_id, task_type, role_category)
  WHERE state = 'UNASSIGNED' AND indexed AND assignee IS NULL;

CREATE INDEX CONCURRENTLY task_search_permissions_authorization_lookup_idx
  ON cft_task_db.task_search_permissions USING btree
    (permission, role_name, authorization_value, task_id)
  WHERE authorization_value IS NOT NULL;

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS task_search_permissions_task_lookup_idx
  ON cft_task_db.task_search_permissions USING btree (
    task_id,
    role_name,
    permission,
    (authorization_value IS NULL),
    (COALESCE(authorization_value, ''))
  )
  INCLUDE (authorization_value);
