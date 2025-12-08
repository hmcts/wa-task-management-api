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
                     assignment_end_reason = l_end_reason,
                     report_refresh_time = current_timestamp
                where task_id = l_task_id and assignment_end is null and assignment_start < l_updated;

            insert into cft_task_db.task_assignments
                (assignment_start, assignee, task_id, service, location, role_category, task_name, report_refresh_time)
              values
                (l_updated, l_assignee, l_task_id, l_jurisdiction, l_location, l_role_category, l_task_name, current_timestamp)
                returning assignment_id into l_update_id;
        end if;
    end if;

    if ((COALESCE(l_assignee,'') <> '') and (l_update_action in ('Complete', 'Terminate', 'Cancel')))
        or ((COALESCE(l_assignee,'') = '') and (l_update_action in ('Unassign', 'Unclaim', 'AutoUnassign'))) then

        update cft_task_db.task_assignments
            set  assignment_end =  l_updated,
                 assignment_end_reason = l_end_reason,
                 report_refresh_time = current_timestamp
            where task_id = l_task_id and assignment_end is null and assignment_start < l_updated;
    else
        update cft_task_db.task_assignments
                    set  report_refresh_time = current_timestamp
        where task_id = l_task_id;
    end if;

return l_update_id;
end $function$;
