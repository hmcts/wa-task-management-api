-- DROP SUBSCRIPTION task_subscription;

-- GRANT CONNECT ON DATABASE cft_task_db_replica TO repl_user;
GRANT USAGE ON SCHEMA cft_task_db TO repl_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA cft_task_db TO repl_user;
