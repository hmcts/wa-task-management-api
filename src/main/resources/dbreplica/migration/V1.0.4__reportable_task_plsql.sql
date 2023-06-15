--
-- Copies a task record into the history table.
--
CREATE OR REPLACE FUNCTION cft_task_db.add_reportable_task(l_task cft_task_db.tasks)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$
declare
  l_update_id bigint;
  l_new_task boolean;
  l_result_action varchar default 'ignore';
  l_update_lock integer;

begin

  -- checking relevant fields are populated relating to a new task being assigned
select not exists(select 1 from cft_task_db.reportable_task where reportable_task.task_id = l_task.task_id)
intersect
select not exists(select 1
                  from cft_task_db.task_history
                  where task_history.task_id = l_task.task_id
                  having count(*) > 1)
into l_new_task;

if (l_task.last_reconfiguration_time is null) and
      ((l_task.last_updated_action = 'Configure' and l_task.state in ('UNASSIGNED', 'ASSIGNED'))
        or (l_task.last_updated_action = 'AutoAssign' and l_task.state = 'ASSIGNED')) then
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
     update_action, created_date, due_date, last_updated_date)
    values
      (l_task.task_id, l_task.task_name, l_task.task_type, l_task.due_date_time,
      l_task.state, l_task.task_system, l_task.security_classification,
      l_task.title, l_task.major_priority, l_task.minor_priority, l_task.assignee,
      l_task.auto_assigned, l_task.execution_type_code, l_task.work_type,
      l_task.role_category, l_task.has_warnings, l_task.assignment_expiry,
      l_task.case_id, l_task.case_type_id, l_task.case_category, l_task.case_name,
      l_task.jurisdiction, l_task.region, l_task.location, l_task.business_context,
      l_task.termination_reason, l_task.created, l_task.last_updated_user, l_task.last_updated_timestamp,
      l_task.last_updated_action, l_task.created::DATE, l_task.due_date_time::date,
      l_task.last_updated_timestamp::date)
    returning update_id into l_update_id;
end if;

if l_result_action = 'update' then
    select 1 into l_update_lock from cft_task_db.reportable_task
    where reportable_task.task_id = l_task.task_id FOR UPDATE;

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
      updated_by = l_task.last_updated_user,
      updated = l_task.last_updated_timestamp,
      update_action = l_task.last_updated_action,
      due_date = l_task.due_date_time::date,
      last_updated_date = l_task.last_updated_timestamp::date,
      completed_date = case when (l_task.last_updated_action='Complete') and (reportable_task.completed_date is null) then l_task.last_updated_timestamp::date end,
      completed_date_time = case when (l_task.last_updated_action='Complete') and (reportable_task.completed_date is null) then l_task.last_updated_timestamp end,
      final_state_label =
          case
            when (l_task.last_updated_action='Complete') and (reportable_task.final_state_label is null) then 'COMPLETED'
            when (l_task.last_updated_action='Cancel') and (reportable_task.final_state_label is null) then 'USER_CANCELLED'
            when (l_task.last_updated_action='AutoCancel') and (reportable_task.final_state_label is null) then 'AUTO_CANCELLED'
          end,
      first_assigned_date =
          case
            when (reportable_task.first_assigned_date is null) and (l_task.assignee is not null) then l_task.last_updated_timestamp::date
            else reportable_task.first_assigned_date
          end,
      first_assigned_date_time =
            case
                when (reportable_task.first_assigned_date_time is null) and (l_task.assignee is not null) then l_task.last_updated_timestamp
                else reportable_task.first_assigned_date_time
            end,
      wait_time_days =
            case
                when (l_task.state='ASSIGNED') then (l_task.last_updated_timestamp::date - reportable_task.created_date)
                else reportable_task.wait_time_days
            end,
      handling_time_days = case when (l_task.last_updated_action='Complete') then (l_task.last_updated_timestamp::date - reportable_task.first_assigned_date) end,
      processing_time_days = case when (l_task.last_updated_action='Complete') then (l_task.last_updated_timestamp::date - l_task.created::date) end,
      is_within_sla =
            case
                when (l_task.last_updated_action='Complete') and (l_task.last_updated_timestamp <= l_task.due_date_time) then 'Yes'
                when (l_task.last_updated_action='Complete') and (l_task.last_updated_timestamp > l_task.due_date_time) then 'No'
            end,
      number_of_reassignments = (select case when count(*) = 0 then 0 else count(*)-1 end from cft_task_db.task_history where task_history.state = 'ASSIGNED' and task_history.task_id =  l_task.task_id),
      due_date_to_completed_diff_days = case when (l_task.last_updated_action='Complete') then (l_task.last_updated_timestamp::date - l_task.due_date_time::date) end,
      wait_time =
            case
                when (reportable_task.wait_time is null) and (l_task.state='ASSIGNED') then (date_trunc('second', l_task.last_updated_timestamp) - date_trunc('second', l_task.created))
                else reportable_task.wait_time
            end,
      handling_time =
            case
                when (reportable_task.handling_time is null) and (l_task.state='COMPLETED') then (date_trunc('second', l_task.last_updated_timestamp) - date_trunc('second', reportable_task.first_assigned_date_time))
                else reportable_task.handling_time
            end,
      processing_time =
            case
                when (reportable_task.processing_time is null) and (l_task.state='COMPLETED') then (date_trunc('second', l_task.last_updated_timestamp) - date_trunc('second', l_task.created))
                else reportable_task.processing_time
            end,
      due_date_to_completed_diff_time =
            case
                when (reportable_task.due_date_to_completed_diff_time is null) and (l_task.state='COMPLETED') then (date_trunc('second', l_task.due_date_time) - date_trunc('second', l_task.last_updated_timestamp))
                else reportable_task.due_date_to_completed_diff_time
            end
    where reportable_task.task_id = l_task.task_id;
end if;

return l_update_id;
end $function$;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create
or replace function cft_task_db.after_task_insert()
  returns trigger
  language plpgsql
as $function$
begin
  perform
cft_task_db.add_reportable_task(new);
return new;
end $function$;

--
-- Function to call from triggers whenever a task record is deleted.
--
create
or replace function cft_task_db.after_task_delete()
  returns trigger
  language plpgsql
as $function$
begin
  perform
cft_task_db.add_reportable_task(old);
return old;
end $function$;

DROP TRIGGER IF EXISTS trg_after_task_insert ON cft_task_db.tasks;
--
-- Add the task insert trigger.
--
CREATE TRIGGER trg_after_task_insert
    after insert or update on cft_task_db.tasks
    for each row when (NEW.case_id is not null) execute function cft_task_db.after_task_insert();
alter table cft_task_db.tasks enable always trigger trg_after_task_insert;

DROP TRIGGER IF EXISTS trg_after_task_delete ON cft_task_db.tasks;
--
-- Add the task upsert trigger.
--
CREATE TRIGGER trg_after_task_delete
    after delete on cft_task_db.tasks
    for each row execute function cft_task_db.after_task_delete();
alter table cft_task_db.tasks enable always trigger trg_after_task_delete;
