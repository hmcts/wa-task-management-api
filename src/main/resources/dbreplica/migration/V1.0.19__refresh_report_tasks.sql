CREATE OR REPLACE PROCEDURE cft_task_db.refresh_report_tasks(
  max_batch_size INTEGER)
  LANGUAGE plpgsql
AS $procedure$

declare
l_task_id_list              TEXT[];
l_task_id                   TEXT;
l_batches                   INTEGER;

begin

    if (max_batch_size is null or max_batch_size < 1) then
      max_batch_size := 10000;
    end if;

    select count(task_id)/max_batch_size+1 FROM cft_task_db.tasks
    WHERE report_refresh_request_time is not null
    INTO STRICT l_batches;

    FOR counter IN 1..l_batches
    LOOP
      select array(select task_id
          FROM cft_task_db.tasks WHERE report_refresh_request_time is not null order by report_refresh_request_time desc
          limit max_batch_size)
          INTO STRICT l_task_id_list;

      foreach l_task_id in ARRAY l_task_id_list
      loop
        perform cft_task_db.add_reportable_task(l_task_id);
        update cft_task_db.reportable_task set report_refresh_time = current_timestamp where task_id = l_task_id;
        perform cft_task_db.add_task_assignments(l_task_id);
        update cft_task_db.task_assignments set report_refresh_time = current_timestamp where task_id = l_task_id;
        update cft_task_db.tasks set report_refresh_request_time = null where task_id = l_task_id;
      end loop;
    END LOOP;

end $procedure$;
