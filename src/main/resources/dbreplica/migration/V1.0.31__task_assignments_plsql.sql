--
-- Task Assignment Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function cft_task_db.task_assignment_after_task_upsert()
  returns trigger
  language plpgsql
as $function$
declare
err_context                       text;
err_message                       text;
err_detail                        text;
begin
  perform cft_task_db.add_task_assignments(new.task_id);
  return new;

  EXCEPTION
    WHEN others THEN
      GET STACKED DIAGNOSTICS err_context = PG_EXCEPTION_CONTEXT,
                                  err_message = MESSAGE_TEXT,
                                  err_detail = PG_EXCEPTION_DETAIL;
      RAISE 'Task Assignment Upsert Error :%',SQLERRM;
      RAISE 'Task Assignment Upsert Error State:%', SQLSTATE;
      RAISE 'Task Assignment Upsert Error Context:%', err_context;
      RAISE 'Task Assignment Upsert Error Message:%', err_message;
      RAISE 'Task Assignment Upsert Error Detail:%', err_detail;

end $function$;
