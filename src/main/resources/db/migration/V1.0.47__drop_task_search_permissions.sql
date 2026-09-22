DROP TRIGGER IF EXISTS refresh_task_search_permissions_on_task_roles
    ON cft_task_db.task_roles;
DROP TRIGGER IF EXISTS refresh_task_search_permissions_on_tasks
    ON cft_task_db.tasks;

DROP FUNCTION IF EXISTS cft_task_db.refresh_task_search_permissions_from_task_roles();
DROP FUNCTION IF EXISTS cft_task_db.refresh_task_search_permissions_from_tasks();
DROP FUNCTION IF EXISTS cft_task_db.refresh_task_search_permissions(TEXT);
DROP FUNCTION IF EXISTS cft_task_db.delete_task_search_permissions(TEXT);

-- The direct task_roles search supersedes this derived table. Dropping the table
-- also removes task_search_permissions_authorization_lookup_idx and
-- task_search_permissions_task_lookup_idx.
DROP TABLE IF EXISTS cft_task_db.task_search_permissions;
