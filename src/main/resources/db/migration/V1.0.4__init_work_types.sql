DROP TABLE IF EXISTS work_types;
CREATE TABLE work_types
(
    work_type_id TEXT,
    label TEXT,
    PRIMARY KEY (work_type_id)
);

INSERT INTO work_types(work_type_id, label) VALUES
    ('hearing_work','Hearing work'),
    ('upper_tribunal','Upper Tribunal'),
    ('routine_work','Routine work'),
    ('decision_making_work','Decision-making work'),
    ('applications','Applications'),
    ('priority','Priority'),
    ('access_requests','Access requests'),
    ('error_management','Error management');
COMMIT;
