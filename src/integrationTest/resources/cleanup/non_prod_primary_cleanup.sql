CREATE OR REPLACE FUNCTION cft_task_db.task_cleanup_between_dates_primary(created_time_from TIMESTAMP,created_time_to TIMESTAMP)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var TEXT;
BEGIN
    -- Loop through the rows of the tasks table
    FOR task_id_var IN SELECT task_id FROM cft_task_db.tasks WHERE created >= created_time_from AND created <= created_time_to order by created limit 2000 LOOP

        -- delete from sensitive_task_event_logs
        DELETE FROM cft_task_db.sensitive_task_event_logs WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.sensitive_task_event_logs WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in sensitive_task_event_logs table. Skipping next steps', task_id_var;
            CONTINUE;
        END IF;

        -- delete from task_roles
        DELETE FROM cft_task_db.task_roles WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_roles WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in task_roles table. Skipping next steps', task_id_var;
            CONTINUE;
        END IF;

        -- delete from tasks
        DELETE FROM cft_task_db.tasks WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in tasks table', task_id_var;
        END IF;

    END LOOP;

    RETURN;

END;
$function$
