-- DROP TABLE IF EXISTS "cft_task_db"."tasks" CASCADE;
CREATE TABLE "cft_task_db"."tasks"
(
  "task_id" Text NOT NULL,
  "task_name" Text,
  "task_type" Text,
  "due_date_time" Timestamp Without Time Zone NOT NULL,
  "state" Text,
  "task_system" Text,
  "security_classification" Text,
  "title" Text,
  "description" Text,
  "notes" JSONB,
  "major_priority" Integer DEFAULT 5000,
  "minor_priority" Integer DEFAULT 500,
  "assignee" Text,
  "auto_assigned" Boolean DEFAULT false,
  "execution_type_code" Text,
  "work_type" Text,
  "role_category" Text,
  "has_warnings" Boolean DEFAULT false,
  "assignment_expiry" Timestamp Without Time Zone,
  "case_id" Text,
  "case_type_id" Text,
  "case_category" Text,
  "case_name" Text,
  "jurisdiction" Text,
  "region" Text,
  "region_name" Text,
  "location" Text,
  "location_name" Text,
  "business_context" Text,
  "termination_reason" Text,
  "created" Timestamp Without Time Zone NOT NULL,
  "additional_properties" JSONB,
  "reconfigure_request_time" Timestamp Without Time Zone,
  "next_hearing_id" Text,
  "next_hearing_date" Timestamp Without Time Zone,
  "priority_date" Timestamp Without Time Zone NOT NULL,
  "last_reconfiguration_time" Timestamp Without Time Zone,
  "last_updated_timestamp" Timestamp Without Time Zone,
  "last_updated_user" Text,
  "last_updated_action" Text,
  PRIMARY KEY ( "task_id" )
);

-- DROP TABLE IF EXISTS tasks CASCADE;
-- CREATE TABLE cft_task_db.tasks
-- (
--   task_id                 TEXT,
--   task_name               TEXT,
--   task_type               TEXT,
--   due_date_time           TIMESTAMP,
--   state                   TEXT,
--   task_system             TEXT,
--   security_classification TEXT,
--   title                   TEXT,
--   description             TEXT,
--   notes                   JSONB,
--   major_priority          INTEGER,
--   minor_priority          INTEGER,
--   assignee                TEXT,
--   auto_assigned           BOOLEAN   default false,
--   execution_type_code     TEXT,
--   work_type               TEXT,
--   role_category           TEXT,
--   has_warnings            BOOLEAN   default false,
--   assignment_expiry       TIMESTAMP,
--   case_id                 TEXT,
--   case_type_id            TEXT,
--   case_category           TEXT,
--   case_name               TEXT,
--   jurisdiction            TEXT,
--   region                  TEXT,
--   region_name             TEXT,
--   location                TEXT,
--   location_name           TEXT,
--   business_context        TEXT,
--   termination_reason      TEXT,
--   created                 TIMESTAMP default CURRENT_TIMESTAMP,
--   updated                 TIMESTAMP default CURRENT_TIMESTAMP,
--   updated_by              TEXT,
--   update_action           TEXT,
--   PRIMARY KEY (task_id)
-- );

-- drop table if exists task_history cascade;
create table cft_task_db.task_history
(
  update_id               SERIAL NOT NULL,
  task_id                 TEXT,
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
  PRIMARY KEY (update_id)
);
create index task_history_update_id_idx on cft_task_db.task_history (task_id, update_id);
create index task_history_updated_idx on cft_task_db.task_history (task_id, updated);

-- /*
--  * Identifies the update record which holds the latest task data.
--  */
-- drop view if exists latest_task_update_id cascade;
-- create view latest_task_update_id as
-- select task_id, max(update_id) as update_id
-- from task_history
-- group by task_id;
--
-- /*
--  * The latest data for every task.
--  */
-- drop view if exists current_task cascade;
-- create view current_task as
-- select h.*
-- from task_history h, latest_task_update_id u
-- where h.update_id = u.update_id
-- and   h.task_id = u.task_id;
--
-- drop table if exists assignment_history cascade;
-- create table assignment_history
-- (
--   id serial not null,
--   task_id  text not null,
--   assignee text,
--   start_at timestamp not null,
--   start_with text not null,
--   end_at timestamp not null,
--   end_with text not null,
--   primary key (id)
-- );
-- create index assignment_history_task_id_start_at_idx on assignment_history (task_id, start_at);
-- create index assignment_history_assignee_idx on assignment_history (assignee);
--
-- drop table if exists task_timings cascade;
-- create table task_timings
-- (
--   task_id text not null,
--   created timestamp,
--   assignment_count integer,
--   assignment_duration_seconds integer,
--   last_assignment_duration_seconds integer,
--   terminated timestamp,
--   termination_reason text,
--   assignee_at_termination text,
--   primary key (task_id)
-- );
