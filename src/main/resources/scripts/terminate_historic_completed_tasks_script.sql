-- Terminate Historic completed tasks

-- The pre_check_tasks function retrieves task IDs in the COMPLETED or CANCELLED states created more than 90 days ago,
-- up to a specified limit. It checks if these tasks exist in replica database tables
-- (tasks, reportable_tasks, and task_history) and logs missing or complete data for each task.
-- The function returns an array of task IDs that meet the criteria.
--
-- Example for calling the function
-- select cft_task_db.pre_check_tasks(100);
CREATE OR REPLACE FUNCTION cft_task_db.pre_check_tasks(max_limit INT DEFAULT 2000)
RETURNS TABLE(task_ids TEXT[], task_count INT)
LANGUAGE plpgsql
AS $function$
DECLARE
    valid_task_ids TEXT[] := '{}';
    valid_task_count INT := 0;
    task_id_var TEXT;
    missing_data BOOLEAN;
BEGIN
    -- Retrieve tasks in COMPLETED or CANCELLED states created before 90 days, limited by max_limit
    FOR task_id_var IN
        SELECT task_id
        FROM cft_task_db.tasks
        WHERE state IN ('COMPLETED', 'CANCELLED')
          AND last_updated_timestamp < NOW() - INTERVAL '90 days'
        ORDER BY created ASC
        LIMIT max_limit
    LOOP
        -- Initialize missing_data flag
        missing_data := FALSE;

        -- Check if required fields are null
        IF EXISTS (
            SELECT 1
            FROM cft_task_db.tasks t
            WHERE t.task_id = task_id_var
              AND (t.created IS NULL
                OR t.last_updated_user IS NULL
                OR t.last_updated_action IS NULL)
        ) THEN
            missing_data := TRUE;
            RAISE INFO 'Missing data for task_id: %', task_id_var;
        END IF;

        -- Add task_id to valid_task_ids if no data is missing
        IF NOT missing_data THEN
            valid_task_ids := array_append(valid_task_ids, '''' || task_id_var || '''');
        END IF;
    END LOOP;

    -- Calculate the count of valid tasks
    valid_task_count := COALESCE(array_length(valid_task_ids, 1), 0);

    -- Return the array of valid task_ids and the count
    RETURN QUERY SELECT valid_task_ids, valid_task_count;
END;
$function$;

-- The terminate_historic_completed_tasks function processes an array of task IDs to terminate tasks in
-- the cft_task_db.tasks table. It validates the input, checks task existence and state, updates the state to
-- TERMINATED with relevant metadata, and logs the changes.
-- Tasks must be in COMPLETED or CANCELLED states and created over 90 days ago to qualify.
--
-- Example for calling the procedure
-- call cft_task_db.terminate_historic_completed_tasks(ARRAY['task_id1', 'task_id2', 'task_id3'],'user_id',100);

CREATE OR REPLACE PROCEDURE cft_task_db.terminate_historic_completed_tasks(
    task_ids TEXT[],
    user_id TEXT DEFAULT 'script',
    max_limit INT DEFAULT 100
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    task_id_var TEXT;
    task_state TEXT;
    updated_count INT;
BEGIN
    -- Initialize the updated_count
    updated_count := 0;

    -- Check if the task_ids array is empty or exceeds the max limit
    IF task_ids IS NULL OR array_length(task_ids, 1) = 0 THEN
        RAISE EXCEPTION 'The task_ids array is empty or null';
    ELSIF array_length(task_ids, 1) > max_limit THEN
        RAISE EXCEPTION 'The number of task_ids exceeds the maximum limit of %', max_limit;
    END IF;

    FOR task_id_var IN
        SELECT UNNEST(task_ids)
    LOOP
        -- Abort with a log message if no record is found
        IF NOT EXISTS (
            SELECT 1
            FROM cft_task_db.tasks t
            WHERE t.task_id = task_id_var
              AND t.state IN ('COMPLETED', 'CANCELLED')
              AND t.last_updated_timestamp < NOW() - INTERVAL '90 days'
        ) THEN
            RAISE INFO 'No record found for task_id: %', task_id_var;
            CONTINUE;
        END IF;

        SELECT t.state INTO task_state
        FROM cft_task_db.tasks t
        WHERE t.task_id = task_id_var;

        -- Update the task state and other fields
        UPDATE cft_task_db.tasks t
        SET state = 'TERMINATED',
            termination_reason = CASE
                WHEN task_state = 'COMPLETED' THEN 'completed'
                WHEN task_state = 'CANCELLED' THEN 'deleted'
            END,
            last_updated_timestamp = NOW(),
            last_updated_user = user_id,
            last_updated_action = 'TerminateException'
        WHERE task_id = task_id_var;

        COMMIT;

        -- Increment the counter for successfully updated tasks
        updated_count := updated_count + 1;

        -- Log the successfully updated task_id
        RAISE INFO 'Successfully updated task_id: %, state: %, termination_reason: %, last_updated_timestamp: %, last_updated_user: %, last_updated_action: %',
            task_id_var, 'TERMINATED',
            task_state,
            NOW(), user_id, 'TerminateException';
    END LOOP;

    -- Log the total count of successfully updated tasks
    RAISE INFO 'Total successfully updated tasks: %', updated_count;
END;
$procedure$;

-- The validate_terminated_tasks function validates the termination details of tasks in the cft_task_db.tasks table
-- for a given array of task IDs. It checks if the termination_reason, last_updated_timestamp, last_updated_user,
-- and last_updated_action fields meet specific criteria. For each task, it logs whether the conditions are valid or
-- if any discrepancies are found. This function ensures that terminated tasks have consistent and accurate metadata.
--
-- Example for calling the function
-- select cft_task_db.validate_terminated_tasks_in_primary(ARRAY['task_id1', 'task_id2', 'task_id3'],'user_id','2023-04-04 11:00'::TIMESTAMP);

CREATE OR REPLACE FUNCTION cft_task_db.validate_terminated_tasks_in_primary(task_ids TEXT[], user_id TEXT DEFAULT 'script', last_updated_script_timestamp TIMESTAMP)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var TEXT;
    termination_reason_valid BOOLEAN;
    timestamp_valid BOOLEAN;
    user_valid BOOLEAN;
    action_valid BOOLEAN;
    state_valid BOOLEAN;
BEGIN
    -- Loop through each task_id in the array
    FOR task_id_var IN SELECT UNNEST(task_ids)
    LOOP
        -- Initialize validation flags
        state_valid := FALSE;
        termination_reason_valid := FALSE;
        timestamp_valid := FALSE;
        user_valid := FALSE;
        action_valid := FALSE;

        -- Check the task details
        SELECT
            (state = 'TERMINATED') AS state_valid,
            (termination_reason IN ('completed', 'deleted')) AS termination_reason_valid,
            (last_updated_timestamp >= last_updated_script_timestamp) AS timestamp_valid,
            (last_updated_user = user_id) AS user_valid,
            (last_updated_action = 'TerminateException') AS action_valid
        INTO
            state_valid,
            termination_reason_valid,
            timestamp_valid,
            user_valid,
            action_valid
        FROM cft_task_db.tasks
        WHERE task_id = task_id_var;

        -- Raise info if any condition is not met
        IF NOT termination_reason_valid THEN
            RAISE INFO 'Task ID %: Invalid termination_reason', task_id_var;
        END IF;

        IF NOT state_valid THEN
             RAISE INFO 'Task ID %: Invalid state', task_id_var;
        END IF;

        IF NOT timestamp_valid THEN
            RAISE INFO 'Task ID %: last_updated_timestamp not within 5 minutes', task_id_var;
        END IF;

        IF NOT user_valid THEN
            RAISE INFO 'Task ID %: last_updated_user mismatch', task_id_var;
        END IF;

        IF NOT action_valid THEN
            RAISE INFO 'Task ID %: last_updated_action mismatch', task_id_var;
        END IF;

        -- Log if all conditions are met
        IF termination_reason_valid AND timestamp_valid AND user_valid AND action_valid THEN
            RAISE INFO 'Task ID %: All conditions are valid', task_id_var;
        END IF;
    END LOOP;
END;
$function$;

-- The validate_terminated_tasks_in_replica function validates the termination details of tasks in the cft_task_db
-- replica database tables (tasks, reportable_tasks, and task_history) for a given array of task IDs.
-- It checks if the termination_reason, last_updated_timestamp, last_updated_user, and last_updated_action fields
-- meet specific criteria. For each task, it logs discrepancies if any conditions are not met and confirms when all
-- conditions are valid across all tables. This ensures consistency and accuracy of metadata for terminated tasks in
-- the replica database.
--
-- Example for calling the function
-- select cft_task_db.validate_terminated_tasks_in_replica(ARRAY['task_id1', 'task_id2', 'task_id3'],'user_id','2023-04-04 11:00'::TIMESTAMP);

CREATE OR REPLACE FUNCTION cft_task_db.validate_terminated_tasks_in_replica(task_ids TEXT[], user_id TEXT DEFAULT 'script', last_updated_script_timestamp TIMESTAMP)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    task_id_var TEXT;
    termination_reason_valid BOOLEAN;
    timestamp_valid BOOLEAN;
    user_valid BOOLEAN;
    action_valid BOOLEAN;
    state_valid BOOLEAN;
BEGIN
    -- Loop through each task_id in the array
    FOR task_id_var IN SELECT UNNEST(task_ids)
    LOOP
        -- Initialize validation flags
        termination_reason_valid := FALSE;
        timestamp_valid := FALSE;
        user_valid := FALSE;
        action_valid := FALSE;
        state_valid := FALSE;

        -- Check the task details in tasks table
        SELECT
            (state = 'TERMINATED') AS state_valid,
            (termination_reason IN ('completed', 'deleted')) AS termination_reason_valid,
            (last_updated_timestamp >= last_updated_script_timestamp) AS timestamp_valid,
            (last_updated_user = user_id) AS user_valid,
            (last_updated_action = 'TerminateException') AS action_valid
        INTO
            state_valid,
            termination_reason_valid,
            timestamp_valid,
            user_valid,
            action_valid
        FROM cft_task_db.tasks
        WHERE task_id = task_id_var;

        -- Raise info if any condition is not met in tasks table

        IF NOT state_valid THEN
                     RAISE INFO 'Task ID %: Invalid state', task_id_var;
                END IF;

        IF NOT termination_reason_valid THEN
            RAISE INFO 'Task ID %: Invalid termination_reason in tasks table', task_id_var;
        END IF;

        IF NOT timestamp_valid THEN
            RAISE INFO 'Task ID %: last_updated_timestamp not within 5 minutes in tasks table', task_id_var;
        END IF;

        IF NOT user_valid THEN
            RAISE INFO 'Task ID %: last_updated_user mismatch in tasks table', task_id_var;
        END IF;

        IF NOT action_valid THEN
            RAISE INFO 'Task ID %: last_updated_action mismatch in tasks table', task_id_var;
        END IF;

        -- Repeat the same checks for reportable_tasks table
        SELECT
            (state = 'TERMINATED') AS state_valid,
            (termination_reason IN ('completed', 'deleted')) AS termination_reason_valid,
            (updated >= last_updated_script_timestamp) AS timestamp_valid,
            (updated_by = user_id) AS user_valid,
            (update_action = 'TerminateException') AS action_valid
        INTO
            state_valid,
            termination_reason_valid,
            timestamp_valid,
            user_valid,
            action_valid
        FROM cft_task_db.reportable_task
        WHERE task_id = task_id_var;

        -- Raise info if any condition is not met in reportable_task table

        IF NOT state_valid THEN
                     RAISE INFO 'Task ID %: Invalid state', task_id_var;
                END IF;

         IF NOT timestamp_valid THEN
                    RAISE INFO 'Task ID %: last_updated_timestamp not within 5 minutes in task_history table', task_id_var;
                END IF;

                IF NOT user_valid THEN
                    RAISE INFO 'Task ID %: last_updated_user mismatch in task_history table', task_id_var;
                END IF;

                IF NOT action_valid THEN
                    RAISE INFO 'Task ID %: last_updated_action mismatch in task_history table', task_id_var;
                END IF;

        IF NOT termination_reason_valid THEN
            RAISE INFO 'Task ID %: Invalid termination_reason in reportable_task table', task_id_var;
        END IF;

        -- Repeat the same checks for task_history table
        SELECT
            (state = 'TERMINATED') AS state_valid,
            (termination_reason IN ('completed', 'deleted')) AS termination_reason_valid,
            (updated >= last_updated_script_timestamp) AS timestamp_valid,
            (updated_by = user_id) AS user_valid,
            (update_action = 'TerminateException') AS action_valid
        INTO
            state_valid,
            termination_reason_valid,
            timestamp_valid,
            user_valid,
            action_valid
        FROM cft_task_db.task_history
        WHERE task_id = task_id_var and state = 'TERMINATED' order by updated desc;

        -- Raise info if any condition is not met in task_history table

        IF NOT state_valid THEN
                     RAISE INFO 'Task ID %: Invalid state', task_id_var;
                END IF;

        IF NOT termination_reason_valid THEN
            RAISE INFO 'Task ID %: Invalid termination_reason in task_history table', task_id_var;
        END IF;

         IF NOT timestamp_valid THEN
                    RAISE INFO 'Task ID %: last_updated_timestamp not within 5 minutes in task_history table', task_id_var;
                END IF;

                IF NOT user_valid THEN
                    RAISE INFO 'Task ID %: last_updated_user mismatch in task_history table', task_id_var;
                END IF;

                IF NOT action_valid THEN
                    RAISE INFO 'Task ID %: last_updated_action mismatch in task_history table', task_id_var;
                END IF;

        -- Log if all conditions are met in all tables
        IF termination_reason_valid AND timestamp_valid AND user_valid AND action_valid THEN
            RAISE INFO 'Task ID %: All conditions are valid in all tables', task_id_var;
        END IF;
    END LOOP;
END;
$function$;
