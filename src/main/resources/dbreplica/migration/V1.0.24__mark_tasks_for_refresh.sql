CREATE OR REPLACE PROCEDURE cft_task_db.mark_report_tasks_for_refresh(
  list_of_task_ids text[],
  list_of_case_ids text[],
  jurisdiction text,
  case_type text,
  list_of_states text[],
  before_time timestamp,
  after_time timestamp)
  LANGUAGE plpgsql
AS $procedure$

declare
l_before_time              TIMESTAMP;
l_after_time               TIMESTAMP;
l_query_filter             text;

begin

  l_before_time := date_trunc('second', before_time);
  l_after_time := date_trunc('second', after_time);

  l_query_filter := concat_ws(' and ',
                    'created < $1',
                    'created > $2',
                    case when array_length(list_of_states, 1) > 0 then
                    'state = ANY($3)'
                    end,
                    case when (jurisdiction is not null) and (jurisdiction <> '') then
                    'jurisdiction = $4'
                    end,
                    case when (case_type is not null) and (case_type <> '') then
                    'case_type_id = $5'
                    end,
                    case when array_length(list_of_task_ids, 1) > 0 then
                    'task_id = ANY($6)'
                    end,
                    case when array_length(list_of_case_ids, 1) > 0 then
                    'case_id = ANY($7)'
                    end);

    execute concat($$ update cft_task_db.tasks set report_refresh_request_time = current_timestamp
                     where $$, l_query_filter) using l_before_time, l_after_time, list_of_states, jurisdiction, case_type, list_of_task_ids, list_of_case_ids;

end $procedure$;
