CREATE OR REPLACE FUNCTION cft_task_db.task_cleanup_between_dates_replica(created_time_from TIMESTAMP,created_time_to TIMESTAMP)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var TEXT;
    created_date timestamp;
BEGIN

    -- Loop through the rows of the tasks table
    FOR task_id_var,created_date IN SELECT DISTINCT task_id,created FROM cft_task_db.task_history WHERE created >= created_time_from AND created <= created_time_to order by created limit 10000 LOOP

        -- Check if any more rows exist in tasks
        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in tasks table. Skipping next steps', task_id_var;
            CONTINUE;
        END IF;

        -- delete from reportable_task
        DELETE FROM cft_task_db.reportable_task WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.reportable_task WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in reportable_task table', task_id_var;
            CONTINUE;
        END IF;

        -- delete from task_assignments
        DELETE FROM cft_task_db.task_assignments WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_assignments WHERE task_id = task_id_var) THEN
          RAISE NOTICE 'Issue with the taskId: % in task_assignments table', task_id_var;
          CONTINUE;
        END IF;

        -- delete from task_history
        DELETE FROM cft_task_db.task_history WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_history WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in task_history table', task_id_var;
        END IF;

    END LOOP;

    RETURN;

END;
$function$
