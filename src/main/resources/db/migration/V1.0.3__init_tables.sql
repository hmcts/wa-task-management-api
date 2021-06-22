DROP TABLE IF EXISTS execution_types;
CREATE TABLE execution_types
(
    execution_code execution_type_enum,
    execution_name TEXT,
    description    TEXT,
    PRIMARY KEY (code)
);

DROP TABLE IF EXISTS tasks;
CREATE TABLE tasks
(
    task_id                 TEXT,
    task_name               TEXT,
    task_type               TEXT,
    due_date_time           TIMESTAMP,
    state                   task_state_enum,
    task_system             task_system_enum,
    security_classification security_classification_enum,
    title                   TEXT,
    description             TEXT,
    notes                   JSONB,
    major_priority          INTEGER,
    minor_priority          INTEGER,
    assignee                TEXT,
    auto_assigned           BOOLEAN   default false,
    execution_type_code     execution_type_enum,
    work_type               TEXT,
    role_category           TEXT,
    has_warnings            BOOLEAN   default false,
    assignment_expiry       TIMESTAMP,
    case_id                 TEXT,
    case_type_id            TEXT,
    case_category           TEXT,
    case_name               TEXT,
    jurisdiction            TEXT,
    region                  TEXT,
    region_name             TEXT,
    location                TEXT,
    location_name           TEXT,
    business_context        business_context_enum,
    created                 TIMESTAMP default CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id),
    CONSTRAINT fk_execution_type_code
        FOREIGN KEY (execution_type_code)
            REFERENCES execution_types (code)
);

DROP TABLE IF EXISTS task_roles;
CREATE TABLE task_roles
(
    task_role_id        UUID,
    read                BOOLEAN   default false,
    own                 BOOLEAN   default false,
    execute             BOOLEAN   default false,
    manage              BOOLEAN   default false,
    cancel              BOOLEAN   default false,
    refer               BOOLEAN   default false,
    authorizations      TEXT[],
    assignment_priority INTEGER,
    auto_assignable     BOOLEAN   default false,
    role_category       TEXT,
    task_id             TEXT,
    created             TIMESTAMP default CURRENT_TIMESTAMP,
    PRIMARY KEY (task_role_id),
    CONSTRAINT fk_task_id
        FOREIGN KEY (task_id)
            REFERENCES tasks (task_id)
);

COMMIT;
