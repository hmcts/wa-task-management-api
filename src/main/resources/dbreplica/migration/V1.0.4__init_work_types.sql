DROP TABLE IF EXISTS work_types;
CREATE TABLE work_types
(
    work_type_id TEXT,
    label TEXT,
    PRIMARY KEY (work_type_id)
);

INSERT INTO work_types(work_type_id, LABEL) VALUES
    ('hearing-work','Hearing work'),
    ('upper-tribunal','Upper Tribunal'),
    ('routine-work','Routine work'),
    ('decision-making-work','Decision-making work'),
    ('applications','Applications'),
    ('priority','Priority'),
    ('access-requests','Access requests'),
    ('error-management','Error management'),
    ('review_case','Review Case'),
    ('evidence','Evidence'),
    ('follow_up','Follow Up');
COMMIT;
