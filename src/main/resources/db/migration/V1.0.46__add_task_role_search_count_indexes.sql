-- Role-first indexes support broad permission semi-joins as well as per-task lookups.
-- Keep permission predicates aligned with TaskRoleSearchPredicate.
CREATE INDEX CONCURRENTLY IF NOT EXISTS task_roles_search_manage_idx
    ON cft_task_db.task_roles (role_name, task_id)
    WHERE manage;

-- Available-task searches also inspect authorizations; include them to avoid heap reads.
CREATE INDEX CONCURRENTLY IF NOT EXISTS task_roles_search_available_idx
    ON cft_task_db.task_roles (role_name, task_id)
    INCLUDE (authorizations)
    WHERE own AND claim;

-- Unlike the existing available-task index, this covers counts with any assignee
-- and either active state. Include every remaining task filter and role-scope field.
CREATE INDEX CONCURRENTLY IF NOT EXISTS search_active_tasks_count_idx
    ON cft_task_db.tasks (state, jurisdiction, work_type)
    INCLUDE (task_id, security_classification, region, location, case_id, task_type, role_category, assignee)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;
