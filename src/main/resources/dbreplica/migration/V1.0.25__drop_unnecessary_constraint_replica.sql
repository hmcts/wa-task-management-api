ALTER TABLE task_assignments
  ALTER COLUMN location DROP NOT NULL;

ALTER TABLE task_assignments
  ALTER COLUMN task_name DROP NOT NULL;


