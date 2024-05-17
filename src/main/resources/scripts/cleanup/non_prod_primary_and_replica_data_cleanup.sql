--
-- RWA-2482 cleanup old tasks from primary task database
--

CREATE OR REPLACE FUNCTION cft_task_db.task_cleanup_between_dates_primary()
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var text;
BEGIN
    -- Loop through the rows of the tasks table
    FOR task_id_var IN SELECT task_id FROM cft_task_db.tasks WHERE created >= '<From Date and time>' AND created <= '<To Date and time>' limit 10000 LOOP

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

--
-- RWA-2482 cleanup old tasks from replica task database
--

CREATE OR REPLACE FUNCTION cft_task_db.task_cleanup_between_dates_replica()
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var text;
BEGIN

    -- Loop through the rows of the tasks table
    FOR task_id_var IN SELECT DISTINCT task_id FROM cft_task_db.task_history WHERE created >= '<From Date and time>' AND created <= '<To Date and time>' limit 10000 LOOP

        -- Check if any more rows exist in tasks
        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in tasks table. Skipping next steps', task_id_var;
            CONTINUE;
        END IF;

        -- delete from reportable_task
        DELETE FROM cft_task_db.reportable_task WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.reportable_task WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in reportable_task table', task_id_var;
        END IF;

        -- delete from task_assignments
        DELETE FROM cft_task_db.task_assignments WHERE task_id = task_id_var;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_assignments WHERE task_id = task_id_var) THEN
          RAISE NOTICE 'Issue with the taskId: % in task_assignments table', task_id_var;
        END IF;

        -- delete from task_history
        DELETE FROM cft_task_db.task_history WHERE task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_history WHERE task_id = task_id_var) THEN
            RAISE NOTICE 'Issue with the taskId: % in task_history table', task_id_var;
        END IF;

    END LOOP;

    RETURN;

END;
$function$
