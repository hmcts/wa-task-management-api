--
-- RWA-2382 Trigger on reportable_task to create and update record in task_assignments for every assignee of task.
--
CREATE OR REPLACE FUNCTION cft_task_db.add_task_assignments(l_task_id text)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id bigint;
l_assignee text;
l_update_action text;
l_updated TIMESTAMP;
l_state text;
l_jurisdiction text;
l_location text;
l_role_category text;
l_task_name text;
task_history_cursor CURSOR FOR
    SELECT assignee, update_action, updated, state, jurisdiction, location, role_category, task_name
    FROM cft_task_db.task_history
    WHERE task_id = l_task_id order by updated;

begin

    OPEN task_history_cursor;

    LOOP
    FETCH NEXT FROM task_history_cursor INTO l_assignee, l_update_action, l_updated, l_state, l_jurisdiction, l_location, l_role_category, l_task_name;

    -- Exit the loop if no more rows are available
    EXIT WHEN NOT FOUND;

    -- Call the function and pass the row data as parameters
    SELECT cft_task_db.upsert_task_assignment(l_task_id, l_assignee, l_update_action, l_updated, l_state, l_jurisdiction, l_location, l_role_category, l_task_name)
        INTO l_update_id;
    END LOOP;

    CLOSE task_history_cursor;

return l_update_id;
end $function$;

-- Insert or update task assignment for a task history record
create or replace function cft_task_db.upsert_task_assignment(l_task_id text, l_assignee text, l_update_action text, l_updated TIMESTAMP, l_state text,
l_jurisdiction text, l_location text, l_role_category text, l_task_name text)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id bigint;
l_end_reason text;
l_new_assignment boolean;

begin

    l_end_reason =
    case
        when l_update_action in ('UnassignAssign','UnassignClaim','UnclaimAssign','AutoUnassignAssign')
            then 'REASSIGNED'
        when l_update_action in ('Unclaim')
            then 'UNCLAIMED'
        when l_update_action in ('AutoUnassign','Unassign')
            then 'UNASSIGNED'
        when l_update_action in ('AutoCancel','Cancel')
            then 'CANCELLED'
        else l_state
    end;

    if ((COALESCE(l_assignee,'') <> '')
      and (l_update_action in ('AutoUnassignAssign', 'UnassignAssign', 'UnassignClaim', 'UnclaimAssign', 'Assign', 'AutoAssign', 'Claim'))) then

        select not exists(select 1 from cft_task_db.task_assignments where task_id = l_task_id and assignee = l_assignee
                                and (assignment_end is null or assignment_start = l_updated))
        into l_new_assignment;

        if (l_new_assignment) then
            update cft_task_db.task_assignments
                set  assignment_end =  l_updated,
                     assignment_end_reason = l_end_reason
                where task_id = l_task_id and assignment_end is null and assignment_start < l_updated;

            insert into cft_task_db.task_assignments
                (assignment_start, assignee, task_id, service, location, role_category, task_name)
              values
                (l_updated, l_assignee, l_task_id, l_jurisdiction, l_location, l_role_category, l_task_name)
                returning assignment_id into l_update_id;
        end if;
    end if;

    if ((COALESCE(l_assignee,'') <> '') and (l_update_action in ('Complete', 'AutoCancel', 'Cancel')))
        or ((COALESCE(l_assignee,'') = '') and (l_update_action in ('Unassign', 'Unclaim', 'AutoUnassign'))) then

        update cft_task_db.task_assignments
            set  assignment_end =  l_updated,
                 assignment_end_reason = l_end_reason
            where task_id = l_task_id and assignment_end is null and assignment_start < l_updated;
    end if;

return l_update_id;
end $function$;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function cft_task_db.task_assignment_after_task_upsert()
  returns trigger
  language plpgsql
as $function$
begin
  perform cft_task_db.add_task_assignments(new.task_id);
return new;
end $function$;

DROP TRIGGER IF EXISTS trg_task_assignment_after_task_upsert ON cft_task_db.tasks;
--
CREATE TRIGGER trg_task_assignment_after_task_upsert after insert or update on cft_task_db.tasks
                                        for each row execute function cft_task_db.task_assignment_after_task_upsert();
alter table cft_task_db.tasks enable always trigger trg_task_assignment_after_task_upsert;
