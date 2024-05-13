--
-- RWA-2482 cleanup old tasks from primary task database
--

CREATE OR REPLACE FUNCTION task_cleanup_primary()
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    id text;
BEGIN
    -- Loop through the rows of the tasks table
    FOR id IN select task_id from cft_task_db.tasks WHERE EXTRACT(YEAR FROM created) in (2021) LOOP

        -- delete from sensitive_task_event_logs
        delete from cft_task_db.sensitive_task_event_logs where task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.sensitive_task_event_logs where task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in sensitive_task_event_logs table', id;
        END IF;

        -- delete from task_roles
        delete from cft_task_db.task_roles where task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_roles where task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in task_roles table', id;
        END IF;

        -- delete from tasks
        DELETE FROM cft_task_db.tasks WHERE task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in tasks table', id;
            RETURN;
        END IF;
    END LOOP;
END;
$function$

--
-- RWA-2482 cleanup old tasks from replica task database
--

CREATE OR REPLACE FUNCTION task_cleanup_replica()
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    id text;
BEGIN
    -- Loop through the rows of the tasks table
    FOR id IN select task_id from cft_task_db.tasks WHERE EXTRACT(YEAR FROM created) in (2021) LOOP

        -- Check if any more rows exist in tasks
        IF EXISTS (SELECT 1 FROM cft_task_db.tasks where task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in tasks table', id;
        END IF;

        -- delete from reportable_task
        delete from cft_task_db.reportable_task where task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.reportable_task where task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in reportable_task table', id;
        END IF;

        -- delete from task_assignments
        delete from cft_task_db.task_assignments where task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_assignments where task_id = id) THEN
          RAISE NOTICE 'Issue with the taskId: % in task_assignments table', id;
        END IF;

        -- delete from task_history
        DELETE FROM cft_task_db.task_history WHERE task_id = id;

        IF EXISTS (SELECT 1 FROM cft_task_db.task_history WHERE task_id = id) THEN
            RAISE NOTICE 'Issue with the taskId: % in task_history table', id;
            RETURN;
        END IF;
    END LOOP;
END;
$function$
