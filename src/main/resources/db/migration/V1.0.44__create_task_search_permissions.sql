/*
 * Store the RBAC side of task search as relational permission facts. This
 * avoids indexing exploded wildcard signatures while keeping the legacy
 * search_index available for comparison.
 */

CREATE TABLE cft_task_db.task_search_permissions
(
    task_id             TEXT NOT NULL,
    role_name           TEXT NOT NULL,
    permission          TEXT NOT NULL,
    authorization_value TEXT,
    CONSTRAINT fk_task_search_permissions_task_id
        FOREIGN KEY (task_id)
            REFERENCES cft_task_db.tasks (task_id)
            ON DELETE CASCADE,
    CONSTRAINT task_search_permissions_permission_check
        CHECK (permission IN ('r', 'm', 'a'))
);

CREATE OR REPLACE FUNCTION cft_task_db.delete_task_search_permissions(l_task_id TEXT)
    RETURNS VOID LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM cft_task_db.task_search_permissions
    WHERE task_id = l_task_id
      AND authorization_value IS NULL;

    DELETE FROM cft_task_db.task_search_permissions
    WHERE task_id = l_task_id
      AND authorization_value IS NOT NULL;
END;
$$;

CREATE OR REPLACE FUNCTION cft_task_db.refresh_task_search_permissions(l_task_id TEXT)
    RETURNS VOID LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM cft_task_db.delete_task_search_permissions(l_task_id);

    INSERT INTO cft_task_db.task_search_permissions (
        task_id,
        role_name,
        permission,
        authorization_value
    )
    SELECT DISTINCT
        permissions.task_id,
        permissions.role_name,
        permissions.permission,
        NULLIF(permissions.authorization, '*')
    FROM cft_task_db.task_permissions permissions
    JOIN cft_task_db.tasks task
      ON task.task_id = permissions.task_id
    WHERE permissions.task_id = l_task_id
      AND task.indexed
      AND permissions.role_name IS NOT NULL
      AND permissions.permission IS NOT NULL;
END;
$$;

CREATE OR REPLACE FUNCTION cft_task_db.refresh_task_search_permissions_from_tasks()
    RETURNS TRIGGER LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM cft_task_db.delete_task_search_permissions(OLD.task_id);
        RETURN OLD;
    END IF;

    PERFORM cft_task_db.refresh_task_search_permissions(NEW.task_id);
    RETURN NEW;
END;
$$;

CREATE TRIGGER refresh_task_search_permissions_on_tasks
    AFTER INSERT OR DELETE OR UPDATE OF indexed
    ON cft_task_db.tasks
    FOR EACH ROW
    EXECUTE FUNCTION cft_task_db.refresh_task_search_permissions_from_tasks();

CREATE OR REPLACE FUNCTION cft_task_db.refresh_task_search_permissions_from_task_roles()
    RETURNS TRIGGER LANGUAGE plpgsql
AS $$
DECLARE
    l_task_ids TEXT[];
    l_task_id TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        l_task_ids := ARRAY[NEW.task_id];
    ELSIF TG_OP = 'DELETE' THEN
        l_task_ids := ARRAY[OLD.task_id];
    ELSIF OLD.task_id IS DISTINCT FROM NEW.task_id THEN
        l_task_ids := ARRAY[OLD.task_id, NEW.task_id];
    ELSE
        l_task_ids := ARRAY[NEW.task_id];
    END IF;

    FOREACH l_task_id IN ARRAY l_task_ids LOOP
        CONTINUE WHEN l_task_id IS NULL;
        PERFORM cft_task_db.refresh_task_search_permissions(l_task_id);
    END LOOP;

    RETURN NULL;
END;
$$;

CREATE TRIGGER refresh_task_search_permissions_on_task_roles
    AFTER INSERT OR DELETE OR UPDATE OF task_id, role_name, read, manage, own, claim, authorizations
    ON cft_task_db.task_roles
    FOR EACH ROW
    EXECUTE FUNCTION cft_task_db.refresh_task_search_permissions_from_task_roles();

INSERT INTO cft_task_db.task_search_permissions (
    task_id,
    role_name,
    permission,
    authorization_value
)
SELECT DISTINCT
    permissions.task_id,
    permissions.role_name,
    permissions.permission,
    NULLIF(permissions.authorization, '*')
FROM cft_task_db.task_permissions permissions
JOIN cft_task_db.tasks task
  ON task.task_id = permissions.task_id
WHERE task.indexed
  AND permissions.role_name IS NOT NULL
  AND permissions.permission IS NOT NULL;

CREATE UNIQUE INDEX task_search_permissions_null_auth_idx
    ON cft_task_db.task_search_permissions (task_id, role_name, permission)
    WHERE authorization_value IS NULL;

CREATE UNIQUE INDEX task_search_permissions_auth_idx
    ON cft_task_db.task_search_permissions (task_id, role_name, permission, authorization_value)
    WHERE authorization_value IS NOT NULL;

CREATE INDEX task_search_permissions_lookup_idx
    ON cft_task_db.task_search_permissions (permission, role_name, task_id, authorization_value);
