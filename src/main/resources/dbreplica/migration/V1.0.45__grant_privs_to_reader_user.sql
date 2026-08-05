-- Grant usage and select privs on Reader user
GRANT USAGE ON SCHEMA analytics TO "${dbReaderUsername}";
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO "${dbReaderUsername}";
