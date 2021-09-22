DROP TABLE IF EXISTS task_role_authorizations;
CREATE TABLE task_role_authorizations
(
    task_role_id UUID,
    authorizations TEXT,
    CONSTRAINT fk_task_role_id
        FOREIGN KEY (task_role_id)
            REFERENCES task_roles(task_role_id)

);
