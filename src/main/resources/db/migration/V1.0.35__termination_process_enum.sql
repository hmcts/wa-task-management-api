DROP TYPE IF EXISTS termination_process_enum;
CREATE TYPE termination_process_enum as ENUM (
    'EXUI_USER_COMPLETION',
    'EXUI_CASE_EVENT_COMPLETION');
