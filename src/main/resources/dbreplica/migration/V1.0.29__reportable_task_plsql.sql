--
-- Reportable Task Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function cft_task_db.reportable_task_after_task_upsert()
  returns trigger
  language plpgsql
as $function$
declare
err_context                       text;
err_message                       text;
err_detail                        text;
begin
  perform cft_task_db.add_reportable_task(new.task_id);
  return new;

  EXCEPTION
    WHEN others THEN
      GET STACKED DIAGNOSTICS err_context = PG_EXCEPTION_CONTEXT,
                                err_message = MESSAGE_TEXT,
                                err_detail = PG_EXCEPTION_DETAIL;
      RAISE 'Reportable Task Upsert Error on task Upsert:%',SQLERRM;
      RAISE 'Reportable Task Upsert Error State:%', SQLSTATE;
      RAISE 'Reportable Task Upsert Error Context:%', err_context;
      RAISE 'Reportable Task Upsert Error Message:%', err_message;
      RAISE 'Reportable Task Upsert Error Detail:%', err_detail;
end $function$;

--
-- Reportable Task Function to call from triggers whenever a task record is deleted.
--
create or replace function cft_task_db.reportable_task_after_task_delete()
  returns trigger
  language plpgsql
as $function$
declare
err_context                       text;
err_message                       text;
err_detail                        text;
begin
  perform cft_task_db.add_reportable_task(old.task_id);
  return old;

  EXCEPTION
    WHEN others THEN
      GET STACKED DIAGNOSTICS err_context = PG_EXCEPTION_CONTEXT,
                                err_message = MESSAGE_TEXT,
                                err_detail = PG_EXCEPTION_DETAIL;
      RAISE 'Reportable Task Upsert Error on Task Delete:%',SQLERRM;
      RAISE 'Reportable Task Upsert Error State:%', SQLSTATE;
      RAISE 'Reportable Task Upsert Error Context:%', err_context;
      RAISE 'Reportable Task Upsert Error Message:%', err_message;
      RAISE 'Reportable Task Upsert Error Detail:%', err_detail;

end $function$;
