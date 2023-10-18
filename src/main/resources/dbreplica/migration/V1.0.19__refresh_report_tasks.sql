CREATE OR REPLACE PROCEDURE cft_task_db.refresh_report_tasks(
  max_rows_to_process INTEGER)
  LANGUAGE plpgsql
AS $procedure$

declare
l_task_id_list              TEXT[];
l_task_id                   TEXT;
l_count_of_records          INTEGER;
l_records_to_process        INTEGER;

begin

    select count(task_id) FROM cft_task_db.tasks where report_refresh_request_time is not null
    into STRICT l_count_of_records;

    if (l_count_of_records > 0) then

      if (max_rows_to_process < 1 or max_rows_to_process > l_count_of_records) then
         l_records_to_process := l_count_of_records;
      else
         l_records_to_process := max_rows_to_process;
      end if;

      select array(select task_id
                  FROM cft_task_db.tasks WHERE report_refresh_request_time is not null order by report_refresh_request_time desc
                  limit l_records_to_process)
                  INTO STRICT l_task_id_list;

      foreach l_task_id in ARRAY l_task_id_list
      loop
          perform cft_task_db.add_reportable_task(l_task_id);
          update cft_task_db.reportable_task set report_refresh_time = current_timestamp where task_id = l_task_id;
          perform cft_task_db.add_task_assignments(l_task_id);
          update cft_task_db.task_assignments set report_refresh_time = current_timestamp where task_id = l_task_id;
          update cft_task_db.tasks set report_refresh_request_time = null where task_id = l_task_id;
          commit;
      END loop;
    end if;

end $procedure$;
