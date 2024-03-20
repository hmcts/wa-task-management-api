--
-- RWA-2382 Trigger on reportable_task to create and update record in task_assignments for every assignee of task.
--
CREATE OR REPLACE FUNCTION cft_task_db.add_task_assignments(l_task_id text)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$

declare
l_update_id     bigint;
l_assignee      text;
l_update_action text;
l_updated       TIMESTAMP;
l_state         text;
l_jurisdiction  text;
l_location      text;
l_role_category text;
l_task_name     text;
l_new_task      boolean;

task_history_cursor CURSOR FOR
    SELECT assignee, update_action, updated, state, jurisdiction, location, role_category, task_name
    FROM cft_task_db.task_history
    WHERE task_id = l_task_id order by updated;

begin

    select not exists(select 1 from cft_task_db.task_assignments where task_assignments.task_id = l_task_id)
    into l_new_task;

    OPEN task_history_cursor;

    LOOP
    FETCH NEXT FROM task_history_cursor INTO l_assignee, l_update_action, l_updated, l_state, l_jurisdiction, l_location, l_role_category, l_task_name;

    -- Exit the loop if no more rows are available
    EXIT WHEN NOT FOUND;

    if (l_new_task) then

        if ((l_update_action = 'Configure' and l_state = 'UNASSIGNED')
            or (l_update_action = 'AutoAssign' and l_state = 'ASSIGNED')) then
            RAISE INFO 'Check to upsert task assignments record for : %', l_task_id;
            -- Call the function and pass the row data as parameters
            SELECT cft_task_db.upsert_task_assignment(l_task_id, l_assignee, l_update_action, l_updated, l_state, l_jurisdiction, l_location, l_role_category, l_task_name)
                INTO l_update_id;
        else
            RAISE WARNING '% : Task with an incomplete history for assignments check and will therefore not be reported on.', l_task_id;
            EXIT;
        end if;

    else
        -- Call the function and pass the row data as parameters
        SELECT cft_task_db.upsert_task_assignment(l_task_id, l_assignee, l_update_action, l_updated, l_state, l_jurisdiction, l_location, l_role_category, l_task_name)
            INTO l_update_id;
    end if;


    END LOOP;

    CLOSE task_history_cursor;

return l_update_id;
end $function$;
