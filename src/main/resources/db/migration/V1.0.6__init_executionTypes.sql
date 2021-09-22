DROP TABLE IF EXISTS execution_types;
CREATE TABLE execution_types
(
    execution_code execution_type_enum,
    execution_name TEXT,
    description    TEXT,
    PRIMARY KEY (execution_code)
);
INSERT INTO execution_types(execution_code, execution_name, description)
VALUES ('MANUAL',
        'Manual',
        'The task is carried out manually, and must be completed by the user in the task management UI.'),
       ('BUILT_IN',
        'Built In',
        'The application through which the task is presented to the user knows how to launch and complete this task, based on its formKey.'),
       ('CASE_EVENT',
        'Case Management Task',
        'The task requires a case management event to be executed by the user. (Typically this will be in CCD.');
COMMIT;
