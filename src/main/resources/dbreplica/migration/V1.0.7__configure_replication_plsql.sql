--
-- Copies a task record into the history table.
--
CREATE OR REPLACE FUNCTION cft_task_db.add_task_history(l_task cft_task_db.tasks, l_is_delete boolean)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$
declare
  l_update_id bigint;
  l_new_task boolean;
  l_result_action varchar default 'ignore';
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
     l_task.termination_reason, l_task.created, l_task.last_updated_user, l_task.last_updated_timestamp,
     l_task.last_updated_action)
    returning update_id into l_update_id;

  select not exists(select 1 from cft_task_db.reportable_task where reportable_task.task_id = l_task.task_id)
            intersect  select not exists (select 1 from cft_task_db.task_history where task_history.task_id = l_task.task_id having count(*) > 1)
    into l_new_task;

  if (l_task.last_updated_action = 'Configure') and (l_task.state = 'UNASSIGNED')
                                                and (l_task.last_reconfiguration_time is null) then
    l_result_action = 'insert';
    if not l_new_task then
      l_result_action = 'update';
    end if;
  end if;

  if (l_result_action = 'ignore') and not l_new_task then
    l_result_action = 'update';
  end if;

  if l_result_action = 'insert' then
    insert into cft_task_db.reportable_task
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
       l_task.termination_reason, l_task.created, l_task.last_updated_user, l_task.last_updated_timestamp,
       l_task.last_updated_action);
  end if;

  if l_result_action = 'update' then
    update cft_task_db.reportable_task
      set   task_name = l_task.task_name,
      task_type = l_task.task_type,
      due_date_time = l_task.due_date_time,
      state = l_task.state,
      task_system = l_task.task_system,
      security_classification = l_task.security_classification,
      title = l_task.title,
      major_priority = l_task.major_priority,
      minor_priority = l_task.minor_priority,
      assignee = l_task.assignee,
      auto_assigned = l_task.auto_assigned,
      execution_type_code = l_task.execution_type_code,
      work_type = l_task.work_type,
      role_category = l_task.role_category,
      has_warnings = l_task.has_warnings,
      assignment_expiry = l_task.assignment_expiry,
      case_id = l_task.case_id,
      case_type_id = l_task.case_type_id,
      case_category = l_task.case_category,
      case_name = l_task.case_name,
      jurisdiction = l_task.jurisdiction,
      region = l_task.region,
      location = l_task.location,
      business_context = l_task.business_context,
      termination_reason = l_task.termination_reason,
      created = l_task.created,
      updated_by = l_task.last_updated_user,
      updated = l_task.last_updated_timestamp,
      update_action = l_task.last_updated_action
    where reportable_task.task_id = l_task.task_id;
  end if;

return l_update_id;
end $function$
;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function on_task_insert()
  returns trigger
  language plpgsql
as $function$
begin
  perform cft_task_db.add_task_history(new, true);
return new;
end $function$;

create or replace function on_task_update()
  returns trigger
  language plpgsql
as $function$
begin
  perform cft_task_db.add_task_history(new, false);
return new;
end $function$;

--
-- Function to call from triggers whenever a task record is deleted.
--
create or replace function on_task_delete()
  returns trigger
  language plpgsql
as $function$
begin
  perform cft_task_db.add_task_history(old, true);
return old;
end $function$;

DROP TRIGGER IF EXISTS trg_on_task_insert ON cft_task_db.tasks;
--
-- Add the task insert trigger.
--
CREATE TRIGGER trg_on_task_insert before insert on cft_task_db.tasks
  for each row when (NEW.case_id is not null) execute function on_task_insert();
alter table cft_task_db.tasks enable always trigger trg_on_task_insert;

DROP TRIGGER IF EXISTS trg_on_task_update ON cft_task_db.tasks;
--
-- Add the task update trigger.
--
CREATE TRIGGER trg_on_task_update before update on cft_task_db.tasks
  for each row when (NEW.case_id is not null) execute function on_task_insert();
alter table cft_task_db.tasks enable always trigger trg_on_task_update;

DROP TRIGGER IF EXISTS trg_on_task_delete ON cft_task_db.tasks;
--
-- Add the task upsert trigger.
--
CREATE TRIGGER trg_on_task_delete before delete on cft_task_db.tasks for each row execute function on_task_delete();
alter table cft_task_db.tasks enable always trigger trg_on_task_delete;

