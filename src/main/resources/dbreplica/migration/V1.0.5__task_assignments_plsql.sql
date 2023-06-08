--
-- RWA-2382 Trigger on reportable_task to create and update record in task_assignments for every assignee of task.
--
CREATE OR REPLACE FUNCTION cft_task_db.add_task_assignments(l_report_task_new cft_task_db.reportable_task,
                                                            l_report_task_old cft_task_db.reportable_task)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id bigint;
  l_assignment_end boolean DEFAULT FALSE;
begin

  if ((COALESCE(l_report_task_old.assignee,'') <> (COALESCE(l_report_task_new.assignee,'')))
      and (l_report_task_new.update_action in
          ('AutoUnassignAssign', 'UnassignAssign', 'UnassignClaim', 'UnclaimAssign', 'Assign'))) then
      l_assignment_end := TRUE;
    end if;

  if ((COALESCE(l_report_task_new.assignee,'') = '')
      and (l_report_task_new.update_action in
          ('Unassign', 'Unclaim', 'AutoUnassign'))) then
      l_assignment_end := TRUE;
    end if;

  if ((COALESCE(l_report_task_old.assignee,'') = (COALESCE(l_report_task_new.assignee,'')))
      and (l_report_task_new.update_action in
          ('Complete', 'AutoCancel', 'Cancel'))) then
      l_assignment_end := TRUE;
    end if;

  if (l_assignment_end) then
    update cft_task_db.task_assignments
    set  assignment_end =  l_report_task_new.updated,
         assignment_end_reason =
           case when l_report_task_new.update_action
                      in ('UnassignAssign','UnassignClaim','UnclaimAssign','AutoUnassignAssign')
                then 'REASSIGNED'
                when l_report_task_new.update_action in ('Unclaim')
                then 'UNCLAIMED'
                when l_report_task_new.update_action in ('AutoUnassign','Unassign')
                then 'UNASSIGNED'
                when l_report_task_new.update_action in ('AutoCancel','Cancel')
                then 'CANCELLED'
                else  l_report_task_new.state
             end
    where task_id = l_report_task_new.task_id
      and   assignee = l_report_task_old.assignee;
    end if;

  if ((l_report_task_new.assignee is NOT NULL)
      and ((COALESCE(l_report_task_old.assignee,'') = '')
                or ((COALESCE(l_report_task_old.assignee,'') <> l_report_task_new.assignee)))
      and (l_report_task_new.update_action in
        ('AutoAssign', 'Assign', 'Claim', 'AutoUnassignAssign',
          'UnassignAssign', 'UnassignClaim', 'UnclaimAssign'))) then

        insert into cft_task_db.task_assignments
          (assignment_start, assignee, task_id, service, location, role_category, task_name)
          values
            (l_report_task_new.updated, l_report_task_new.assignee, l_report_task_new.task_id,
              l_report_task_new.jurisdiction, l_report_task_new.location,
              l_report_task_new.role_category, l_report_task_new.task_name)
            returning assignment_id into l_update_id;
    end if;

return l_update_id;
end $function$
;

--
-- Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function on_report_task_insert()
  returns trigger
  language plpgsql
as $function$
begin
  perform cft_task_db.add_task_assignments(new, old);
return new;
end $function$;

DROP TRIGGER IF EXISTS trg_on_report_task_insert ON cft_task_db.reportable_task;
DELETE from cft_task_db.reportable_task;
--
CREATE TRIGGER trg_on_report_task_insert after insert or update on cft_task_db.reportable_task
                                                           for each row execute function on_report_task_insert();
alter table cft_task_db.reportable_task enable always trigger trg_on_report_task_insert;
