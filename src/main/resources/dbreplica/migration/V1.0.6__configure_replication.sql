GRANT USAGE ON SCHEMA cft_task_db TO repl_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA cft_task_db TO repl_user;

create subscription task_subscription
  connection 'host=localhost port=5432 dbname=cft_task_db user=repl_user password=repl_password'
  publication task_publication
  WITH (slot_name = test_slot_v1, create_slot = false);
