CREATE OR REPLACE FUNCTION cft_task_db.add_reportable_task(l_task_id text)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id bigint;
l_new_task boolean;
l_task_name               TEXT;
l_task_type               TEXT;
l_due_date_time           TIMESTAMP;
l_state                   TEXT;
l_task_system             TEXT;
l_security_classification TEXT;
l_title                   TEXT;
l_major_priority          INTEGER;
l_minor_priority          INTEGER;
l_assignee                TEXT;
l_auto_assigned           BOOLEAN;
l_execution_type_code     TEXT;
l_work_type               TEXT;
l_role_category           TEXT;
l_has_warnings            BOOLEAN;
l_assignment_expiry       TIMESTAMP;
l_case_id                 TEXT;
l_case_type_id            TEXT;
l_case_category           TEXT;
l_case_name               TEXT;
l_jurisdiction            TEXT;
l_region                  TEXT;
l_location                TEXT;
l_business_context        TEXT;
l_termination_reason      TEXT;
l_created                 TIMESTAMP;
l_updated_by              TEXT;
l_updated                 TIMESTAMP;
l_update_action           TEXT;
l_due_date_to_completed_diff_days    INTEGER;
l_completed_date          DATE;
l_completed_date_time     TIMESTAMP;
l_final_state_label       TEXT;
l_first_assigned_date     DATE;
l_first_assigned_date_time     TIMESTAMP;
l_number_of_reassignments      INTEGER := -1;
l_wait_time_days          INTEGER;
l_handling_time_days      INTEGER;
l_processing_time_days    INTEGER;
l_is_within_sla           TEXT;
l_wait_time                       INTERVAL;
l_handling_time                   INTERVAL;
l_processing_time                 INTERVAL;
l_due_date_to_completed_diff_time INTERVAL;


task_history_cursor CURSOR FOR
    SELECT task_id,task_name,task_type,due_date_time,state,task_system,security_classification,title,major_priority,
        minor_priority,assignee,auto_assigned,execution_type_code,work_type,role_category,has_warnings,assignment_expiry,
        case_id,case_type_id,case_category,case_name,jurisdiction,region,location,business_context,termination_reason,
        created,updated_by,updated,update_action
    FROM cft_task_db.task_history
    WHERE task_id = l_task_id order by updated;

begin

select not exists(select 1 from cft_task_db.reportable_task where reportable_task.task_id = l_task_id)
into l_new_task;

OPEN task_history_cursor;

LOOP
FETCH NEXT FROM task_history_cursor INTO
    l_task_id,l_task_name,l_task_type,l_due_date_time,l_state,l_task_system,l_security_classification,l_title,l_major_priority,
    l_minor_priority,l_assignee,l_auto_assigned,l_execution_type_code,l_work_type,l_role_category,l_has_warnings,l_assignment_expiry,
    l_case_id,l_case_type_id,l_case_category,l_case_name,l_jurisdiction,l_region,l_location,l_business_context,l_termination_reason,
    l_created,l_updated_by,l_updated,l_update_action;

    -- Exit the loop if no more rows are available
    EXIT WHEN NOT FOUND;

    if (l_new_task) then
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
              (l_task_id, l_task_name, l_task_type, l_due_date_time,
              l_state, l_task_system, l_security_classification,
              l_title, l_major_priority, l_minor_priority, l_assignee,
              l_auto_assigned, l_execution_type_code, l_work_type,
              l_role_category, l_has_warnings, l_assignment_expiry,
              l_case_id, l_case_type_id, l_case_category, l_case_name,
              l_jurisdiction, l_region, l_location, l_business_context,
              l_termination_reason, l_created, l_updated_by, l_updated,
              l_update_action, l_created::DATE, l_due_date_time::DATE,
              l_updated::DATE)
        returning update_id into l_update_id;
        l_new_task = false;
    else
        if (l_update_action='Complete') then l_completed_date = l_updated::date; end if;
        if (l_update_action='Complete') then l_completed_date_time = l_updated; end if;
        l_final_state_label =
          case
            when (l_update_action='Complete') then 'COMPLETED'
            when (l_update_action='Cancel') then 'USER_CANCELLED'
            when (l_update_action='AutoCancel') then 'AUTO_CANCELLED'
          end;
        if (l_first_assigned_date is null) and (l_assignee is not null) then l_first_assigned_date = l_updated::date; end if;
        if (l_first_assigned_date_time is null) and (l_assignee is not null) then l_first_assigned_date_time = l_updated; end if;
        if (l_state='ASSIGNED') then l_wait_time_days = (l_updated::date - l_created::date); end if;
        if (l_update_action='Complete') and (l_first_assigned_date is not null) then l_handling_time_days = (l_updated::date - l_first_assigned_date); end if;
        if (l_update_action='Complete') then l_processing_time_days = (l_updated::date - l_created::date); end if;
        l_is_within_sla =
            case
                when (l_update_action='Complete') and (l_updated <= l_due_date_time) then 'Yes'
                when (l_update_action='Complete') and (l_updated > l_due_date_time) then 'No'
            end;
        if (l_update_action in ('AutoUnassignAssign', 'UnassignAssign', 'UnassignClaim', 'UnclaimAssign', 'Assign', 'AutoAssign', 'Claim')) then l_number_of_reassignments = l_number_of_reassignments + 1; end if;
        if (l_update_action='Complete') then l_due_date_to_completed_diff_days = (l_updated::date - l_due_date_time::date); end if;
        if (l_wait_time is null) and (l_state='ASSIGNED') then l_wait_time = (date_trunc('second', l_updated) - date_trunc('second', l_created)); end if;
        if (l_handling_time is null) and (l_state='COMPLETED') then l_handling_time = (date_trunc('second', l_updated) - date_trunc('second', l_first_assigned_date_time)); end if;
        if (l_processing_time is null) and (l_state='COMPLETED') then l_processing_time = (date_trunc('second', l_updated) - date_trunc('second', l_created)); end if;
        if (l_due_date_to_completed_diff_time is null) and (l_state='COMPLETED') then l_due_date_to_completed_diff_time = (date_trunc('second', l_due_date_time) - date_trunc('second', l_updated)); end if;

        update cft_task_db.reportable_task
        set   task_name = l_task_name,
              task_type = l_task_type,
              due_date_time = l_due_date_time,
              state = l_state,
              task_system = l_task_system,
              security_classification = l_security_classification,
              title = l_title,
              major_priority = l_major_priority,
              minor_priority = l_minor_priority,
              assignee = l_assignee,
              auto_assigned = l_auto_assigned,
              execution_type_code = l_execution_type_code,
              work_type = l_work_type,
              role_category = l_role_category,
              has_warnings = l_has_warnings,
              assignment_expiry = l_assignment_expiry,
              case_id = l_case_id,
              case_type_id = l_case_type_id,
              case_category = l_case_category,
              case_name = l_case_name,
              jurisdiction = l_jurisdiction,
              region = l_region,
              location = l_location,
              business_context = l_business_context,
              termination_reason = l_termination_reason,
              updated_by = l_updated_by,
              updated = l_updated,
              update_action = l_update_action,
              due_date = l_due_date_time::date,
              last_updated_date = l_updated::date,
              completed_date = l_completed_date::date,
              completed_date_time = l_completed_date,
              final_state_label = l_final_state_label,
              first_assigned_date = l_first_assigned_date,
              first_assigned_date_time = l_first_assigned_date_time,
              wait_time_days = l_wait_time_days,
              handling_time_days = l_handling_time_days,
              processing_time_days = l_processing_time_days,
              is_within_sla = l_is_within_sla,
              number_of_reassignments = case when (l_number_of_reassignments = -1) then 0 else l_number_of_reassignments end,
              due_date_to_completed_diff_days = l_due_date_to_completed_diff_days,
              wait_time = l_wait_time,
              handling_time = l_handling_time,
              processing_time = l_processing_time,
              due_date_to_completed_diff_time = l_due_date_to_completed_diff_time
        where reportable_task.task_id = l_task_id;

    end if;

END LOOP;

CLOSE task_history_cursor;

return l_update_id;
end $function$;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create
or replace function cft_task_db.reportable_task_after_task_upsert()
  returns trigger
  language plpgsql
as $function$
begin
  perform
cft_task_db.add_reportable_task(new.task_id);
return new;
end $function$;

--
-- Function to call from triggers whenever a task record is deleted.
--
create
or replace function cft_task_db.reportable_task_after_task_delete()
  returns trigger
  language plpgsql
as $function$
begin
  perform
cft_task_db.add_reportable_task(old.task_id);
return old;
end $function$;

DROP TRIGGER IF EXISTS trg_reportable_task_after_task_upsert ON cft_task_db.tasks;
--
-- Add the task insert trigger.
--
CREATE TRIGGER trg_reportable_task_after_task_upsert
    after insert or update on cft_task_db.tasks
    for each row when (NEW.case_id is not null) execute function cft_task_db.reportable_task_after_task_upsert();
alter table cft_task_db.tasks enable always trigger trg_reportable_task_after_task_upsert;

DROP TRIGGER IF EXISTS trg_reportable_task_after_task_delete ON cft_task_db.tasks;
--
-- Add the task upsert trigger.
--
CREATE TRIGGER trg_reportable_task_after_task_delete
    after delete on cft_task_db.tasks
    for each row execute function cft_task_db.reportable_task_after_task_delete();
alter table cft_task_db.tasks enable always trigger trg_reportable_task_after_task_delete;
