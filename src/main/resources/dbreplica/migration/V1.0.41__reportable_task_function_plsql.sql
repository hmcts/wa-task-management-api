CREATE OR REPLACE FUNCTION cft_task_db.add_reportable_task(l_task_id text)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id                       bigint;
l_new_task                        boolean;
l_task_name                       TEXT;
l_task_type                       TEXT;
l_due_date_time                   TIMESTAMP;
l_state                           TEXT;
l_task_system                     TEXT;
l_security_classification         TEXT;
l_title                           TEXT;
l_description                     TEXT;
l_notes                           JSONB;
l_major_priority                  INTEGER;
l_minor_priority                  INTEGER;
l_assignee                        TEXT;
l_auto_assigned                   BOOLEAN;
l_execution_type_code             TEXT;
l_work_type                       TEXT;
l_role_category                   TEXT;
l_has_warnings                    BOOLEAN;
l_assignment_expiry               TIMESTAMP;
l_case_id                         TEXT;
l_case_type_id                    TEXT;
l_case_category                   TEXT;
l_case_name                       TEXT;
l_jurisdiction                    TEXT;
l_region                          TEXT;
l_region_name                     TEXT;
l_location                        TEXT;
l_location_name                   TEXT;
l_business_context                TEXT;
l_termination_reason              TEXT;
l_created                         TIMESTAMP;
l_updated_by                      TEXT;
l_updated                         TIMESTAMP;
l_update_action                   TEXT;
l_final_state_label               TEXT;
l_first_assigned_date             DATE;
l_first_assigned_date_time        TIMESTAMP;
l_number_of_reassignments         INTEGER := 0;
l_wait_time_days                  INTEGER;
l_wait_time                       INTERVAL;
l_state_label                     TEXT;
l_role_category_label             TEXT;
l_jurisdiction_label              TEXT;
l_case_type_label                 TEXT;
l_additional_properties           JSONB;
l_reconfigure_request_time        Timestamp;
l_next_hearing_id                 Text;
l_next_hearing_date               Timestamp;
l_priority_date                   Timestamp;
l_last_reconfiguration_time       Timestamp;
l_termination_process             TEXT;
l_termination_process_label       TEXT;
l_assignments                        INTEGER;
l_rt_update_action                   TEXT;
l_rt_final_state_label               TEXT;
l_rt_wait_time_days                  INTEGER;
l_rt_handling_time_days              INTEGER;
l_rt_processing_time_days            INTEGER;
l_rt_is_within_sla                   TEXT;
l_rt_due_date_to_completed_diff_days INTEGER;
l_rt_completed_date                  DATE;
l_rt_completed_date_time             TIMESTAMP;
l_rt_first_assigned_date             DATE;
l_rt_first_assigned_date_time        TIMESTAMP;
l_rt_number_of_reassignments         INTEGER;
l_rt_due_date                        DATE;
l_rt_last_updated_date               DATE;
l_rt_wait_time                       INTERVAL;
l_rt_handling_time                   INTERVAL;
l_rt_processing_time                 INTERVAL;
l_rt_due_date_to_completed_diff_time INTERVAL;
l_rt_agent_name                       TEXT;
l_rt_outcome                          TEXT;

task_history_cursor CURSOR FOR
SELECT task_id,task_name,task_type,due_date_time,state,task_system,security_classification,title,description,notes,
       major_priority,minor_priority,assignee,auto_assigned,execution_type_code,work_type,role_category,has_warnings,
       assignment_expiry,case_id,case_type_id,case_category,case_name,jurisdiction,region,region_name,location,
       location_name,business_context,termination_reason,created,updated_by,updated,update_action,additional_properties,
       reconfigure_request_time,next_hearing_id,next_hearing_date,priority_date,last_reconfiguration_time,termination_process
       FROM cft_task_db.task_history
WHERE task_id = l_task_id order by updated;

begin

select not exists(select 1 from cft_task_db.reportable_task where reportable_task.task_id = l_task_id)
into l_new_task;

OPEN task_history_cursor;

LOOP
FETCH NEXT FROM task_history_cursor INTO
    l_task_id,l_task_name,l_task_type,l_due_date_time,l_state,l_task_system,l_security_classification,l_title,l_description,l_notes,
    l_major_priority,l_minor_priority,l_assignee,l_auto_assigned,l_execution_type_code,l_work_type,l_role_category,l_has_warnings,
    l_assignment_expiry,l_case_id,l_case_type_id,l_case_category,l_case_name,l_jurisdiction,l_region,l_region_name,l_location,
    l_location_name,l_business_context,l_termination_reason,l_created,l_updated_by,l_updated,l_update_action,l_additional_properties,
    l_reconfigure_request_time,l_next_hearing_id,l_next_hearing_date,l_priority_date,l_last_reconfiguration_time,l_termination_process;

    -- Exit the loop if no more rows are available
    EXIT WHEN NOT FOUND;

    l_role_category_label =
        case l_role_category IS NOT NULL
            when (l_role_category='LEGAL_OPERATIONS')  then 'Legal Operations'
            when (l_role_category='CTSC')  then 'CTSC'
            when (l_role_category='JUDICIAL')  then 'Judicial'
            when (l_role_category='ADMIN')  then 'Admin'
            else l_role_category
        end;

    l_jurisdiction_label =
        case l_jurisdiction IS NOT NULL
            when (l_jurisdiction='PRIVATELAW')  then 'Private Law'
            when (l_jurisdiction='CIVIL')  then 'Civil'
            when (l_jurisdiction='IA')  then 'Immigration and Asylum'
            when (l_jurisdiction='PUBLICLAW')  then 'Public Law'
            when (l_jurisdiction='EMPLOYMENT')  then 'Employment'
            when (l_jurisdiction='ST_CIC')  then 'Special Tribunals CIC'
            else l_jurisdiction
        end;

    l_case_type_label =
        case l_case_type_id IS NOT NULL
           when (l_case_type_id='Asylum')  then 'Asylum'
           when (l_case_type_id='CIVIL')  then 'Civil'
           when (l_case_type_id='PRLAPPS')  then 'Private Law'
           when (l_case_type_id='PUBLICLAW')  then 'Public Law'
           when (l_case_type_id='CriminalInjuriesCompensation')  then 'Criminal Injuries Compensation'
           else l_case_type_id
        end;

    l_state_label =
        case l_state IS NOT NULL
            when (l_state='ASSIGNED') then 'Assigned'
            when (l_state='UNASSIGNED') then 'Unassigned'
            when (l_state='COMPLETED') then 'Completed'
            when (l_state='CANCELLED') then 'Cancelled'
            when (l_state='TERMINATED') then 'Terminated'
            when (l_state='PENDING_RECONFIGURATION') then 'Pending Reconfiguration'
            else l_state
        end;

    l_termination_process_label =
        case l_termination_process IS NOT NULL
            when (l_termination_process='EXUI_USER_COMPLETION')  then 'Manual Completion'
            when (l_termination_process='EXUI_CASE_EVENT_COMPLETION')  then 'Automated Completion'
            when (l_termination_process='EXUI_USER_CANCELLATION')  then 'Manual Cancellation'
            when (l_termination_process='EXUI_CASE_EVENT_CANCELLATION')  then 'Automated Cancellation'
            else l_termination_process
        end;

    RAISE INFO 'Inserting sample data into table X';
    RAISE NOTICE 'Inserting sample data into table X2';


    if (l_new_task) then
        RAISE INFO 'Attempting to insert reportable task record for : %', l_task_id;
        if ((l_update_action = 'Configure' and l_state = 'UNASSIGNED')
            or (l_update_action = 'AutoAssign' and l_state = 'ASSIGNED')) then
          RAISE INFO 'Insert reportable task record for : %', l_task_id;
        else
          RAISE WARNING '% : Task with an incomplete history and will therefore not be reported on.', l_task_id;
          EXIT;
        end if;

       if (l_update_action='AutoAssign') and (l_state='ASSIGNED') then
            l_first_assigned_date = l_updated::date;
            l_first_assigned_date_time = l_updated;
            l_wait_time_days = 0;
            l_wait_time = 0;
            l_number_of_reassignments = 0;
       end if;
        RAISE INFO 'Inserting reportable task record for : %', l_task_id;
        insert into cft_task_db.reportable_task
            (task_id, task_name, task_type, due_date_time,
             state, task_system, security_classification,
             title, description, notes, major_priority,
             minor_priority, assignee, auto_assigned,
             execution_type_code, work_type, role_category,
             has_warnings, assignment_expiry, case_id,
             case_type_id, case_category, case_name,
             jurisdiction, region, region_name,
             location, location_name, business_context,
             termination_reason, created, updated_by, updated,
             update_action, created_date, due_date, last_updated_date,
             first_assigned_date, first_assigned_date_time, wait_time_days,
             wait_time, number_of_reassignments, state_label,
             role_category_label, jurisdiction_label, case_type_label,
             additional_properties, reconfigure_request_time, next_hearing_id,
             next_hearing_date, priority_date, last_reconfiguration_time,
             termination_process, termination_process_label, report_refresh_time)
            values
              (l_task_id, l_task_name, l_task_type, l_due_date_time,
              l_state, l_task_system, l_security_classification,
              l_title, l_description, l_notes, l_major_priority,
              l_minor_priority, l_assignee, l_auto_assigned,
              l_execution_type_code, l_work_type, l_role_category,
              l_has_warnings, l_assignment_expiry, l_case_id,
              l_case_type_id, l_case_category, l_case_name,
              l_jurisdiction, l_region, l_region_name,
              l_location, l_location_name, l_business_context,
              l_termination_reason, l_created, l_updated_by, l_updated,
              l_update_action, l_created::DATE, l_due_date_time::DATE,
              l_updated::DATE, l_first_assigned_date, l_first_assigned_date_time,
              l_wait_time_days, l_wait_time, l_number_of_reassignments,
              l_state_label, l_role_category_label, l_jurisdiction_label,
              l_case_type_label, l_additional_properties, l_reconfigure_request_time,
              l_next_hearing_id, l_next_hearing_date, l_priority_date,
              l_last_reconfiguration_time, l_termination_process, l_termination_process_label, current_timestamp)
        returning update_id into l_update_id;
        RAISE INFO 'Inserted reportable task record for : %', l_task_id;
        l_new_task = false;
    else
        RAISE INFO 'Attempting to update reportable task record for : %', l_task_id;
        SELECT update_action, final_state_label, wait_time_days, handling_time_days, processing_time_days,
               is_within_sla, due_date_to_completed_diff_days, completed_date, completed_date_time, first_assigned_date,
               first_assigned_date_time, number_of_reassignments, due_date, last_updated_date, wait_time,
               handling_time, processing_time, due_date_to_completed_diff_time, outcome, agent_name INTO STRICT
               l_rt_update_action, l_rt_final_state_label, l_rt_wait_time_days, l_rt_handling_time_days, l_rt_processing_time_days,
               l_rt_is_within_sla, l_rt_due_date_to_completed_diff_days, l_rt_completed_date, l_rt_completed_date_time, l_rt_first_assigned_date,
               l_rt_first_assigned_date_time, l_rt_number_of_reassignments, l_rt_due_date, l_rt_last_updated_date, l_rt_wait_time,
               l_rt_handling_time, l_rt_processing_time, l_rt_due_date_to_completed_diff_time, l_rt_outcome, l_rt_agent_name
        FROM cft_task_db.reportable_task WHERE task_id = l_task_id;

        SELECT count(*) INTO STRICT l_assignments FROM cft_task_db.task_history WHERE task_id = l_task_id AND
            update_action in ('AutoUnassignAssign', 'UnassignAssign', 'UnassignClaim', 'UnclaimAssign', 'Assign', 'AutoAssign', 'Claim');

        l_rt_number_of_reassignments = l_assignments - 1;

        l_rt_final_state_label =
          case
            when (l_update_action='Complete') then 'COMPLETED'
            when (l_update_action='Cancel') then 'USER_CANCELLED'
            when (l_update_action='AutoCancel') then 'AUTO_CANCELLED'
            else l_rt_final_state_label
          end;

        if (l_rt_first_assigned_date is null) and (l_assignee is not null) and (l_assignee <> '') then
          l_rt_first_assigned_date = l_updated::date;
        end if;

        if (l_rt_first_assigned_date_time is null) and (l_assignee is not null) and (l_assignee <> '') then
          l_rt_first_assigned_date_time = l_updated;
        end if;

        if (l_state='ASSIGNED' and (l_rt_wait_time_days is null)) then
          l_rt_wait_time_days = (l_updated::date - l_created::date);
        end if;

        if (l_state='ASSIGNED' and (l_rt_wait_time is null)) then
          l_rt_wait_time = (date_trunc('second', l_updated) - date_trunc('second', l_created));
        end if;

        if (l_update_action='Complete') then
          l_rt_completed_date = l_updated::date;
          l_rt_completed_date_time = l_updated;
          l_rt_processing_time_days = (l_updated::date - l_created::date);
          l_rt_processing_time = (date_trunc('second', l_updated) - date_trunc('second', l_created));
          l_rt_handling_time_days = (l_updated::date - l_rt_first_assigned_date);
          l_rt_handling_time = (date_trunc('second', l_updated) - date_trunc('second', l_rt_first_assigned_date_time));
          l_rt_due_date_to_completed_diff_days = (l_updated::date - l_due_date_time::date);
          l_rt_due_date_to_completed_diff_time = (date_trunc('second', l_due_date_time) - date_trunc('second', l_updated));
          l_rt_is_within_sla = case when (l_updated <= l_due_date_time) then 'Yes' else 'No' end;
          l_rt_outcome    := 'Completed';
          l_rt_agent_name := l_updated_by;
        end if;

        if l_update_action = 'Cancel' then
          l_rt_outcome    := 'Cancelled';
          l_rt_agent_name := l_updated_by;
        end if;

        if l_state = 'TERMINATED' then
          if l_rt_agent_name is null then
              l_rt_agent_name := l_updated_by;
          end if;

          if l_rt_outcome is null then
              l_rt_outcome :=
                  case l_termination_reason
                      when 'completed' then 'Completed'
                      else 'Cancelled'
                  end;
           end if;
        end if;

        update cft_task_db.reportable_task
        set   task_name = l_task_name,
              task_type = l_task_type,
              due_date_time = l_due_date_time,
              state = l_state,
              task_system = l_task_system,
              security_classification = l_security_classification,
              title = l_title,
              description = l_description,
              notes = l_notes,
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
              region_name = l_region_name,
              location = l_location,
              location_name = l_location_name,
              business_context = l_business_context,
              termination_reason = l_termination_reason,
              updated_by = l_updated_by,
              updated = l_updated,
              update_action = l_update_action,
              due_date = l_due_date_time::date,
              last_updated_date = l_updated::date,
              completed_date = l_rt_completed_date::date,
              completed_date_time = l_rt_completed_date,
              final_state_label = l_rt_final_state_label,
              first_assigned_date = l_rt_first_assigned_date,
              first_assigned_date_time = l_rt_first_assigned_date_time,
              wait_time_days = l_rt_wait_time_days,
              handling_time_days = l_rt_handling_time_days,
              processing_time_days = l_rt_processing_time_days,
              is_within_sla = l_rt_is_within_sla,
              number_of_reassignments = case when (l_rt_number_of_reassignments = -1) then 0 else l_rt_number_of_reassignments end,
              due_date_to_completed_diff_days = l_rt_due_date_to_completed_diff_days,
              wait_time = l_rt_wait_time,
              handling_time = l_rt_handling_time,
              processing_time = l_rt_processing_time,
              due_date_to_completed_diff_time = l_rt_due_date_to_completed_diff_time,
              state_label = l_state_label,
              role_category_label = l_role_category_label,
              jurisdiction_label = l_jurisdiction_label,
              case_type_label = l_case_type_label,
              additional_properties = l_additional_properties,
              reconfigure_request_time = l_reconfigure_request_time,
              next_hearing_id = l_next_hearing_id,
              next_hearing_date = l_next_hearing_date,
              priority_date = l_priority_date,
              last_reconfiguration_time = l_last_reconfiguration_time,
              termination_process = l_termination_process,
              termination_process_label = l_termination_process_label,
              report_refresh_time = current_timestamp,
              outcome = l_rt_outcome,
              agent_name = l_rt_agent_name
        where reportable_task.task_id = l_task_id;

    end if;

END LOOP;

CLOSE task_history_cursor;

return l_update_id;
end $function$;

