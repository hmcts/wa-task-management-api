-- Grant usage and select privs on Reader user
GRANT USAGE ON SCHEMA cft_task_db TO "${dbReaderUsername}";
GRANT SELECT ON ALL TABLES IN SCHEMA cft_task_db TO "${dbReaderUsername}";
