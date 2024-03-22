--
-- Task History Function to call from triggers whenever a task record is inserted or updated.
--
create or replace function cft_task_db.on_task_insert()
  returns trigger
  language plpgsql
as $function$
declare
err_context                       text;
err_message                       text;
err_detail                        text;
begin
  perform cft_task_db.add_task_history(new);
  return new;

  EXCEPTION
    WHEN others THEN
      RAISE 'Task History Insert Error on Task Upsert:%',SQLERRM;
      RAISE 'Task History Insert Error State:%', SQLSTATE;
      RAISE 'Task Assignment Upsert Error Context:%', err_context;
      RAISE 'Task Assignment Upsert Error Message:%', err_message;
      RAISE 'Task Assignment Upsert Error Detail:%', err_detail;

end $function$;

--
-- Task History Function to call from triggers whenever a task record is deleted.
--
create or replace function cft_task_db.on_task_delete()
  returns trigger
  language plpgsql
as $function$
declare
err_context                       text;
err_message                       text;
err_detail                        text;
begin
  perform cft_task_db.add_task_history(old);
  return old;

  EXCEPTION
    WHEN others THEN
      RAISE 'Task History Insert Error on Task Delete:%',SQLERRM;
      RAISE 'Task History Insert Error State:%', SQLSTATE;
      RAISE 'Task Assignment Upsert Error Context:%', err_context;
      RAISE 'Task Assignment Upsert Error Message:%', err_message;
      RAISE 'Task Assignment Upsert Error Detail:%', err_detail;

end $function$;
