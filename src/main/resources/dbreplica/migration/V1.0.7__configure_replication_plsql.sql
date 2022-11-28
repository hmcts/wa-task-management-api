--
-- Copies a task record into the history table.
--
create or replace function cft_task_db.add_task_history(l_task cft_task_db.tasks, l_is_delete boolean)
  returns bigint
  language plpgsql
as $$
declare
l_update_id bigint;
  -- l_updated_by text := case when l_is_delete then null else l_task.updated_by end;
  -- l_updated timestamp := case when l_is_delete then now() else l_task.updated end;
  -- l_update_action text := case when l_is_delete then 'DELETE' else l_task.update_action end;
  l_updated_by text := case when l_is_delete then null else 'tasks.updated_by field to be added' end;
  l_updated timestamp := case when l_is_delete then now() else now() end;
  l_update_action text := case when l_is_delete then 'DELETE' else 'tasks.update_action field to be added' end;
begin
insert into cft_task_db.task_history
(task_id, task_name, task_type, due_date_time,
 state, task_system, security_classification,
 title, major_priority, minor_priority, assignee,
 auto_assigned, execution_type_code, work_type,
 role_category, has_warnings, assignment_expiry,
 case_id, case_type_id, case_category, case_name,
 jurisdiction, region, location, business_context,
 termination_reason, created, updated_by, updated,
 update_action)
values
  (l_task.task_id, l_task.task_name, l_task.task_type, l_task.due_date_time,
   l_task.state, l_task.task_system, l_task.security_classification,
   l_task.title, l_task.major_priority, l_task.minor_priority, l_task.assignee,
   l_task.auto_assigned, l_task.execution_type_code, l_task.work_type,
   l_task.role_category, l_task.has_warnings, l_task.assignment_expiry,
   l_task.case_id, l_task.case_type_id, l_task.case_category, l_task.case_name,
   l_task.jurisdiction, l_task.region, l_task.location, l_task.business_context,
   l_task.termination_reason, l_task.created, l_updated_by, l_updated,
   l_update_action)
  returning update_id into l_update_id;
return l_update_id;
end $$;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function on_task_upsert()
  returns trigger
  language plpgsql
as $$
begin
  perform cft_task_db.add_task_history(new, false);
return new;
end $$;

--
-- Function to call from triggers whenever a task record is deleted.
--
create or replace function on_task_delete()
  returns trigger
  language plpgsql
as $$
begin
  perform cft_task_db.add_task_history(old, true);
return old;
end $$;

DROP TRIGGER IF EXISTS trg_on_task_upsert ON cft_task_db.tasks;
--
-- Add the task upsert trigger.
--
CREATE TRIGGER trg_on_task_upsert before insert or update on cft_task_db.tasks
  for each row execute function on_task_upsert();
alter table cft_task_db.tasks enable always trigger trg_on_task_upsert;

DROP TRIGGER IF EXISTS trg_on_task_delete ON cft_task_db.tasks;
--
-- Add the task upsert trigger.
--
CREATE TRIGGER trg_on_task_delete before delete on cft_task_db.tasks for each row execute function on_task_delete();
alter table cft_task_db.tasks enable always trigger trg_on_task_delete;

