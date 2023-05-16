
-- drop table if exists reportable_task cascade;
create table cft_task_db.reportable_task
(
  update_id               SERIAL NOT NULL,
  task_id                 TEXT NOT NULL,
  task_name               TEXT,
  task_type               TEXT,
  due_date_time           TIMESTAMP,
  state                   TEXT,
  task_system             TEXT,
  security_classification TEXT,
  title                   TEXT,
  major_priority          INTEGER,
  minor_priority          INTEGER,
  assignee                TEXT,
  auto_assigned           BOOLEAN,
  execution_type_code     TEXT,
  work_type               TEXT,
  role_category           TEXT,
  has_warnings            BOOLEAN,
  assignment_expiry       TIMESTAMP,
  case_id                 TEXT,
  case_type_id            TEXT,
  case_category           TEXT,
  case_name               TEXT,
  jurisdiction            TEXT,
  region                  TEXT,
  location                TEXT,
  business_context        TEXT,
  termination_reason      TEXT,
  created                 TIMESTAMP,
  updated_by              TEXT,
  updated                 TIMESTAMP,
  update_action           TEXT,
  PRIMARY KEY ( "task_id" )
);