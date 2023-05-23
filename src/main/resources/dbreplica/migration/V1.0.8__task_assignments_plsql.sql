drop table if exists task_assignments cascade;
create table cft_task_db.task_assignments
(
  assignment_id               SERIAL NOT NULL,
  assignment_start            TIMESTAMP NOT NULL,
  assignment_end              TIMESTAMP,
  assignee                    TEXT NOT NULL,
  task_id                     TEXT NOT NULL,
  service                     TEXT NOT NULL,
  location                    TEXT NOT NULL,
  role_category               TEXT NOT NULL,
  task_name                   TEXT NOT NULL,
  assignment_end_reason       TEXT,
  PRIMARY KEY ( "assignment_id" )
);
