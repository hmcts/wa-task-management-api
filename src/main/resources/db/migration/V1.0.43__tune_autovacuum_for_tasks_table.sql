ALTER TABLE cft_task_db.tasks SET (
    autovacuum_vacuum_scale_factor = 0.02,
    autovacuum_analyze_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 5000,
    autovacuum_analyze_threshold = 5000
);
