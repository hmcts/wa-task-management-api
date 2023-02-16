DROP TABLE IF EXISTS work_types;
CREATE TABLE work_types
(
    id TEXT,
    label TEXT,
    PRIMARY KEY (id)
);

INSERT INTO work_types(ID, LABEL) VALUES
    ('hearing-work','Hearing work'),
    ('upper-tribunal','Upper Tribunal'),
    ('routine-work','Routine work'),
    ('decision-making-work','Decision-making work'),
    ('applications','Applications'),
    ('priority','Priority'),
    ('access-requests','Access requests'),
    ('error-management','Error management');
COMMIT;
