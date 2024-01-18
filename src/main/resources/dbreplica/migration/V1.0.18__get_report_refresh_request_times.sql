CREATE OR REPLACE FUNCTION cft_task_db.get_report_refresh_request_times(
  list_of_task_ids text[])
  RETURNS timestamp[]
 LANGUAGE plpgsql
AS $function$

declare
l_report_refresh_request_times              TIMESTAMP[];

begin

    select array(select report_refresh_request_time
        FROM cft_task_db.tasks WHERE task_id = ANY(list_of_task_ids)) INTO STRICT l_report_refresh_request_times;
    RETURN l_report_refresh_request_times;

end $function$;
