CREATE SCHEMA IF NOT EXISTS analytics;

-- ============================================================================
-- Multi-snapshot full-rebuild analytics model with immutable snapshot reads.
-- This script is intentionally rerunnable from scratch via explicit drops.
-- ============================================================================

-- Snapshot metadata
CREATE SEQUENCE analytics.snapshot_id_seq;

CREATE TABLE analytics.snapshot_batches (
  snapshot_id BIGINT PRIMARY KEY,
  started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
  completed_at TIMESTAMPTZ,
  status TEXT NOT NULL CHECK (status IN ('running', 'succeeded', 'failed')),
  error_message TEXT
);

CREATE TABLE analytics.snapshot_state (
  -- Single-row control table: tracks current publish pointer.
  singleton_id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton_id),
  published_snapshot_id BIGINT REFERENCES analytics.snapshot_batches(snapshot_id),
  published_at TIMESTAMPTZ,
  in_progress_snapshot_id BIGINT REFERENCES analytics.snapshot_batches(snapshot_id)
);

INSERT INTO analytics.snapshot_state (singleton_id) VALUES (TRUE);

-- Snapshot data tables (immutable rows keyed by snapshot_id)
-- snapshot_task_rows and snapshot_task_daily_facts are partitioned by
-- snapshot_id so each refresh can bulk-load one per-snapshot partition, build
-- local indexes once, then attach atomically.
CREATE TABLE analytics.snapshot_task_rows (
  snapshot_id BIGINT NOT NULL REFERENCES analytics.snapshot_batches(snapshot_id) ON DELETE CASCADE,
  LIKE cft_task_db.reportable_task INCLUDING DEFAULTS,
  within_due_sort_value SMALLINT
) PARTITION BY LIST (snapshot_id);

CREATE TABLE analytics.snapshot_user_completed_facts (
  snapshot_id BIGINT NOT NULL REFERENCES analytics.snapshot_batches(snapshot_id) ON DELETE CASCADE,
  assignee TEXT,
  jurisdiction_label TEXT,
  role_category_label TEXT,
  region TEXT,
  location TEXT,
  task_name TEXT,
  work_type TEXT,
  completed_date DATE,
  tasks INTEGER NOT NULL,
  within_due INTEGER NOT NULL,
  beyond_due INTEGER NOT NULL,
  handling_time_sum NUMERIC,
  handling_time_count INTEGER NOT NULL,
  days_beyond_sum NUMERIC,
  days_beyond_count INTEGER NOT NULL
);

CREATE TABLE analytics.snapshot_task_daily_facts (
  snapshot_id BIGINT NOT NULL REFERENCES analytics.snapshot_batches(snapshot_id) ON DELETE CASCADE,
  date_role TEXT NOT NULL,
  reference_date DATE,
  jurisdiction_label TEXT,
  role_category_label TEXT,
  region TEXT,
  location TEXT,
  task_name TEXT,
  work_type TEXT,
  priority BIGINT,
  task_status TEXT NOT NULL,
  assignment_state TEXT,
  sla_flag BOOLEAN,
  handling_time_days_sum NUMERIC NOT NULL,
  handling_time_days_count BIGINT NOT NULL,
  processing_time_days_sum NUMERIC NOT NULL,
  processing_time_days_count BIGINT NOT NULL,
  task_count BIGINT NOT NULL
) PARTITION BY LIST (snapshot_id);

CREATE TABLE analytics.snapshot_wait_time_by_assigned_date (
  snapshot_id BIGINT NOT NULL REFERENCES analytics.snapshot_batches(snapshot_id) ON DELETE CASCADE,
  jurisdiction_label TEXT,
  role_category_label TEXT,
  region TEXT,
  location TEXT,
  task_name TEXT,
  work_type TEXT,
  reference_date DATE,
  total_wait_time INTERVAL,
  assigned_task_count BIGINT NOT NULL
);

CREATE TABLE analytics.snapshot_filter_facet_facts (
  snapshot_id BIGINT NOT NULL REFERENCES analytics.snapshot_batches(snapshot_id) ON DELETE CASCADE,
  jurisdiction_label TEXT,
  role_category_label TEXT,
  region TEXT,
  location TEXT,
  task_name TEXT,
  work_type TEXT,
  assignee TEXT,
  row_count BIGINT NOT NULL
);

-- Autovacuum tuning for high-churn snapshot tables.
-- snapshot_task_rows and snapshot_task_daily_facts are partitioned and use
-- default per-partition autovacuum settings; refresh runs explicit ANALYZE on
-- each new partition before publish.

ALTER TABLE analytics.snapshot_user_completed_facts
  SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_analyze_threshold = 1000
  );

ALTER TABLE analytics.snapshot_wait_time_by_assigned_date
  SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_analyze_threshold = 1000
  );

ALTER TABLE analytics.snapshot_filter_facet_facts
  SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_analyze_threshold = 1000
  );

-- Snapshot indexes
CREATE INDEX ix_snapshot_task_rows_snapshot_slicers
  ON analytics.snapshot_task_rows(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type
  );

CREATE INDEX ix_snapshot_task_rows_snapshot_state_created_desc
  ON analytics.snapshot_task_rows(snapshot_id, state, created_date DESC);

CREATE INDEX ix_snapshot_task_rows_snapshot_completed_reason_date_desc
  ON analytics.snapshot_task_rows(snapshot_id, LOWER(termination_reason), completed_date DESC);

CREATE INDEX ix_snapshot_task_rows_snapshot_completed_assignee_date_desc
  ON analytics.snapshot_task_rows(snapshot_id, assignee, completed_date DESC)
  WHERE LOWER(termination_reason) = 'completed' AND assignee IS NOT NULL;

CREATE INDEX ix_snapshot_task_rows_snapshot_case_id
  ON analytics.snapshot_task_rows(snapshot_id, case_id);

CREATE INDEX ix_snapshot_task_rows_snapshot_assignee
  ON analytics.snapshot_task_rows(snapshot_id, assignee);

CREATE INDEX ix_snapshot_task_rows_snapshot_upper_role_category
  ON analytics.snapshot_task_rows(snapshot_id, UPPER(role_category_label));

CREATE INDEX ix_snapshot_task_rows_snapshot_within_due_sort
  ON analytics.snapshot_task_rows(snapshot_id, within_due_sort_value, completed_date);

CREATE INDEX ix_snapshot_task_rows_snapshot_open_due_date
  ON analytics.snapshot_task_rows(snapshot_id, due_date)
  WHERE state NOT IN ('COMPLETED', 'TERMINATED');

CREATE UNIQUE INDEX ux_snapshot_user_completed_facts_snapshot_key
  ON analytics.snapshot_user_completed_facts(
    snapshot_id,
    assignee,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type,
    completed_date
  );

CREATE INDEX ix_snapshot_user_completed_facts_snapshot_assignee_date
  ON analytics.snapshot_user_completed_facts(snapshot_id, assignee, completed_date DESC);

CREATE INDEX ix_snapshot_user_completed_facts_snapshot_assignee_task_name
  ON analytics.snapshot_user_completed_facts(snapshot_id, assignee, task_name);

CREATE INDEX ix_snapshot_user_completed_facts_snapshot_slicers
  ON analytics.snapshot_user_completed_facts(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type
  );

CREATE INDEX ix_snapshot_user_completed_facts_snapshot_completed_date
  ON analytics.snapshot_user_completed_facts(snapshot_id, completed_date);

CREATE INDEX ix_snapshot_user_completed_facts_snapshot_upper_role_category
  ON analytics.snapshot_user_completed_facts(snapshot_id, UPPER(role_category_label));

CREATE UNIQUE INDEX ux_snapshot_task_daily_facts_snapshot_key
  ON analytics.snapshot_task_daily_facts(
    snapshot_id,
    date_role,
    reference_date,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type,
    priority,
    task_status,
    assignment_state,
    sla_flag
  );

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_date_role_status_date
  ON analytics.snapshot_task_daily_facts(snapshot_id, date_role, task_status, reference_date);

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_due_open_date
  ON analytics.snapshot_task_daily_facts(snapshot_id, reference_date)
  WHERE date_role = 'due' AND task_status = 'open';

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_created_open_date_assignment
  ON analytics.snapshot_task_daily_facts(snapshot_id, reference_date, assignment_state)
  WHERE date_role = 'created' AND task_status = 'open';

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_slicers
  ON analytics.snapshot_task_daily_facts(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type
  );

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_priority
  ON analytics.snapshot_task_daily_facts(snapshot_id, priority);

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_assignment_state
  ON analytics.snapshot_task_daily_facts(snapshot_id, assignment_state);

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_sla_flag
  ON analytics.snapshot_task_daily_facts(snapshot_id, sla_flag);

CREATE INDEX ix_snapshot_task_daily_facts_snapshot_upper_role_category
  ON analytics.snapshot_task_daily_facts(snapshot_id, UPPER(role_category_label));

CREATE UNIQUE INDEX ux_snapshot_wait_time_by_assigned_date_snapshot_key
  ON analytics.snapshot_wait_time_by_assigned_date(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type,
    reference_date
  );

CREATE INDEX ix_snapshot_wait_time_by_assigned_date_snapshot_slicers
  ON analytics.snapshot_wait_time_by_assigned_date(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type
  );

CREATE INDEX ix_snapshot_wait_time_by_assigned_date_snapshot_reference_date
  ON analytics.snapshot_wait_time_by_assigned_date(snapshot_id, reference_date);

CREATE INDEX ix_snapshot_wait_time_by_assigned_date_snapshot_upper_role_category
  ON analytics.snapshot_wait_time_by_assigned_date(snapshot_id, UPPER(role_category_label));

CREATE UNIQUE INDEX ux_snapshot_filter_facet_facts_snapshot_dims
  ON analytics.snapshot_filter_facet_facts(
    snapshot_id,
    COALESCE(jurisdiction_label, ''),
    COALESCE(role_category_label, ''),
    COALESCE(region, ''),
    COALESCE(location, ''),
    COALESCE(task_name, ''),
    COALESCE(work_type, ''),
    COALESCE(assignee, '')
  );

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_slicers
  ON analytics.snapshot_filter_facet_facts(
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type,
    assignee
  );

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_service
  ON analytics.snapshot_filter_facet_facts(snapshot_id, jurisdiction_label);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_role_category
  ON analytics.snapshot_filter_facet_facts(snapshot_id, role_category_label);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_region
  ON analytics.snapshot_filter_facet_facts(snapshot_id, region);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_location
  ON analytics.snapshot_filter_facet_facts(snapshot_id, location);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_task_name
  ON analytics.snapshot_filter_facet_facts(snapshot_id, task_name);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_work_type
  ON analytics.snapshot_filter_facet_facts(snapshot_id, work_type);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_assignee
  ON analytics.snapshot_filter_facet_facts(snapshot_id, assignee);

CREATE INDEX ix_snapshot_filter_facet_facts_snapshot_upper_role_category
  ON analytics.snapshot_filter_facet_facts(snapshot_id, UPPER(role_category_label));

CREATE OR REPLACE PROCEDURE analytics.refresh_snapshot_filter_facet_facts(p_snapshot_id BIGINT)
LANGUAGE plpgsql
AS $$
BEGIN
  DELETE FROM analytics.snapshot_filter_facet_facts
  WHERE snapshot_id = p_snapshot_id;

  INSERT INTO analytics.snapshot_filter_facet_facts (
    snapshot_id,
    jurisdiction_label,
    role_category_label,
    region,
    location,
    task_name,
    work_type,
    assignee,
    row_count
  )
  SELECT
    p_snapshot_id,
    NULLIF(BTRIM(jurisdiction_label), '') AS jurisdiction_label,
    NULLIF(BTRIM(role_category_label), '') AS role_category_label,
    NULLIF(BTRIM(region), '') AS region,
    NULLIF(BTRIM(location), '') AS location,
    NULLIF(BTRIM(task_name), '') AS task_name,
    NULLIF(BTRIM(work_type), '') AS work_type,
    NULLIF(BTRIM(assignee), '') AS assignee,
    COUNT(*)::BIGINT AS row_count
  FROM analytics.snapshot_task_rows
  WHERE snapshot_id = p_snapshot_id
  GROUP BY
    NULLIF(BTRIM(jurisdiction_label), ''),
    NULLIF(BTRIM(role_category_label), ''),
    NULLIF(BTRIM(region), ''),
    NULLIF(BTRIM(location), ''),
    NULLIF(BTRIM(task_name), ''),
    NULLIF(BTRIM(work_type), ''),
    NULLIF(BTRIM(assignee), '');
END;
$$;

-- Snapshot producer and publisher
CREATE OR REPLACE PROCEDURE analytics.run_snapshot_refresh_batch()
LANGUAGE plpgsql
AS $$
DECLARE
  v_snapshot_id BIGINT;
  v_lock_key BIGINT := hashtext('analytics_run_snapshot_refresh_batch_lock');
  v_batch_failed BOOLEAN := FALSE;
  v_batch_error_message TEXT;
  v_task_rows_partition_name TEXT;
  v_task_daily_partition_name TEXT;
  v_drop_snapshot_id BIGINT;
  v_prev_work_mem TEXT;
  v_prev_hash_mem_multiplier TEXT;
  v_prev_enable_sort TEXT;
BEGIN
  IF NOT pg_try_advisory_lock(v_lock_key) THEN
    RAISE NOTICE 'Analytics snapshot batch already running; skipping trigger.';
    RETURN;
  END IF;

  BEGIN
    v_snapshot_id := nextval('analytics.snapshot_id_seq');

    INSERT INTO analytics.snapshot_batches (
      snapshot_id,
      status
    )
    VALUES (
      v_snapshot_id,
      'running'
    );

    UPDATE analytics.snapshot_state
    SET in_progress_snapshot_id = v_snapshot_id
    WHERE singleton_id = TRUE;
  EXCEPTION
    WHEN OTHERS THEN
      PERFORM pg_advisory_unlock(v_lock_key);
      RAISE;
  END;

  COMMIT;

  BEGIN
    -- Keep heavy refresh aggregations/index builds in memory where possible.
    PERFORM set_config('work_mem', '256MB', TRUE);
    PERFORM set_config('maintenance_work_mem', '1GB', TRUE);

    IF EXISTS (SELECT 1 FROM cft_task_db.reportable_task LIMIT 1) THEN
      CREATE TEMP TABLE tmp_source_full
      ON COMMIT DROP
      AS
      SELECT
        source.task_id,
        source.update_id,
        source.task_name,
        source.jurisdiction_label,
        source.case_type_label,
        source.role_category_label,
        source.case_id,
        source.region,
        source.location,
        source.state,
        source.termination_reason,
        source.termination_process_label,
        source.outcome,
        source.work_type,
        source.is_within_sla,
        source.created_date,
        source.due_date,
        source.completed_date,
        source.due_date_to_completed_diff_time,
        source.first_assigned_date,
        source.major_priority,
        source.assignee,
        source.wait_time_days,
        source.wait_time,
        source.handling_time_days,
        source.handling_time,
        source.processing_time_days,
        source.processing_time,
        source.number_of_reassignments,
        CASE
          WHEN source.is_within_sla = 'Yes' THEN 1
          WHEN source.is_within_sla = 'No' THEN 2
          ELSE 3
        END AS within_due_sort_value
      FROM cft_task_db.reportable_task source;

      v_task_rows_partition_name := format('snapshot_task_rows_p_%s', v_snapshot_id);

      EXECUTE format(
        'CREATE TABLE analytics.%I (
           LIKE analytics.snapshot_task_rows INCLUDING DEFAULTS INCLUDING CONSTRAINTS,
           CHECK (snapshot_id = %s)
         )',
        v_task_rows_partition_name,
        v_snapshot_id
      );

      EXECUTE format(
        'INSERT INTO analytics.%I (
           snapshot_id,
           task_id,
           update_id,
           task_name,
           jurisdiction_label,
           case_type_label,
           role_category_label,
           case_id,
           region,
           location,
           state,
           termination_reason,
           termination_process_label,
           outcome,
           work_type,
           is_within_sla,
           created_date,
           due_date,
           completed_date,
           due_date_to_completed_diff_time,
           first_assigned_date,
           major_priority,
           assignee,
           wait_time_days,
           wait_time,
           handling_time_days,
           handling_time,
           processing_time_days,
           processing_time,
           number_of_reassignments,
           within_due_sort_value
         )
         SELECT
           %s,
           source.task_id,
           source.update_id,
           source.task_name,
           source.jurisdiction_label,
           source.case_type_label,
           source.role_category_label,
           source.case_id,
           source.region,
           source.location,
           source.state,
           source.termination_reason,
           source.termination_process_label,
           source.outcome,
           source.work_type,
           source.is_within_sla,
           source.created_date,
           source.due_date,
           source.completed_date,
           source.due_date_to_completed_diff_time,
           source.first_assigned_date,
           source.major_priority,
           source.assignee,
           source.wait_time_days,
           source.wait_time,
           source.handling_time_days,
           source.handling_time,
           source.processing_time_days,
           source.processing_time,
           source.number_of_reassignments,
           source.within_due_sort_value
         FROM tmp_source_full source',
        v_task_rows_partition_name,
        v_snapshot_id
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, jurisdiction_label, role_category_label, region, location, task_name, work_type)',
        format('ix_str_p_%s_slicers', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, state, created_date DESC)',
        format('ix_str_p_%s_state_created', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, LOWER(termination_reason), completed_date DESC)',
        format('ix_str_p_%s_completed_reason_date', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, assignee, completed_date DESC) WHERE LOWER(termination_reason) = ''completed'' AND assignee IS NOT NULL',
        format('ix_str_p_%s_completed_assignee_date', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, case_id)',
        format('ix_str_p_%s_case_id', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, assignee)',
        format('ix_str_p_%s_assignee', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, UPPER(role_category_label))',
        format('ix_str_p_%s_upper_role_category', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, within_due_sort_value, completed_date)',
        format('ix_str_p_%s_within_due_sort', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, due_date) WHERE state NOT IN (''COMPLETED'', ''TERMINATED'')',
        format('ix_str_p_%s_open_due_date', v_snapshot_id),
        v_task_rows_partition_name
      );

      EXECUTE format(
        'ALTER TABLE analytics.snapshot_task_rows ATTACH PARTITION analytics.%I FOR VALUES IN (%s)',
        v_task_rows_partition_name,
        v_snapshot_id
      );

      EXECUTE format('ANALYZE analytics.%I', v_task_rows_partition_name);

      INSERT INTO analytics.snapshot_user_completed_facts (
        snapshot_id,
        assignee,
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        completed_date,
        tasks,
        within_due,
        beyond_due,
        handling_time_sum,
        handling_time_count,
        days_beyond_sum,
        days_beyond_count
      )
      SELECT
        v_snapshot_id,
        assignee,
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        completed_date::date AS completed_date,
        COUNT(*)::int AS tasks,
        SUM(CASE WHEN is_within_sla = 'Yes' THEN 1 ELSE 0 END)::int AS within_due,
        SUM(CASE WHEN is_within_sla = 'No' THEN 1 ELSE 0 END)::int AS beyond_due,
        SUM(EXTRACT(EPOCH FROM handling_time) / EXTRACT(EPOCH FROM INTERVAL '1 day'))::numeric AS handling_time_sum,
        COUNT(handling_time)::int AS handling_time_count,
        SUM(
          CASE
            WHEN due_date IS NOT NULL AND completed_date IS NOT NULL THEN completed_date::date - due_date::date
            ELSE 0
          END
        )::numeric AS days_beyond_sum,
        SUM(CASE WHEN due_date IS NOT NULL AND completed_date IS NOT NULL THEN 1 ELSE 0 END)::int AS days_beyond_count
      FROM analytics.snapshot_task_rows
      WHERE snapshot_id = v_snapshot_id
        AND completed_date IS NOT NULL
        AND LOWER(termination_reason) = 'completed'
      GROUP BY
        assignee,
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        completed_date::date;

      SELECT
        current_setting('work_mem'),
        current_setting('hash_mem_multiplier'),
        current_setting('enable_sort')
      INTO
        v_prev_work_mem,
        v_prev_hash_mem_multiplier,
        v_prev_enable_sort;

      -- Bias task-daily aggregation toward in-memory hash aggregate to avoid
      -- external sort spill on larger snapshots.
      PERFORM set_config('work_mem', '1GB', TRUE);
      PERFORM set_config('hash_mem_multiplier', '4', TRUE);
      PERFORM set_config('enable_sort', 'off', TRUE);
      v_task_daily_partition_name := format('snapshot_task_daily_facts_p_%s', v_snapshot_id);

      EXECUTE format(
        'CREATE TABLE analytics.%I (
           LIKE analytics.snapshot_task_daily_facts INCLUDING DEFAULTS INCLUDING CONSTRAINTS,
           CHECK (snapshot_id = %s)
         )',
        v_task_daily_partition_name,
        v_snapshot_id
      );

      EXECUTE format(
        $task_daily_insert$
        INSERT INTO analytics.%I (
          snapshot_id,
          date_role,
          reference_date,
          jurisdiction_label,
          role_category_label,
          region,
          location,
          task_name,
          work_type,
          priority,
          task_status,
          assignment_state,
          sla_flag,
          handling_time_days_sum,
          handling_time_days_count,
          processing_time_days_sum,
          processing_time_days_count,
          task_count
        )
        WITH base AS (
          SELECT
            task_name,
            jurisdiction_label,
            role_category_label,
            region,
            location,
            work_type,
            major_priority AS priority,
            state,
            termination_reason,
            CASE
              WHEN is_within_sla = 'Yes' THEN TRUE
              WHEN is_within_sla = 'No' THEN FALSE
              ELSE NULL
            END AS within_sla,
            created_date,
            due_date,
            completed_date,
            handling_time,
            processing_time
          FROM analytics.snapshot_task_rows
          WHERE snapshot_id = $1
        )
        SELECT
          $1,
          'due'::text AS date_role,
          due_date AS reference_date,
          jurisdiction_label,
          role_category_label,
          region,
          location,
          task_name,
          work_type,
          priority,
          CASE
            WHEN LOWER(termination_reason) = 'completed' THEN 'completed'
            WHEN state IN ('ASSIGNED', 'UNASSIGNED', 'PENDING AUTO ASSIGN', 'UNCONFIGURED') THEN 'open'
            ELSE 'other'
          END AS task_status,
          CASE
            WHEN state = 'ASSIGNED' THEN 'Assigned'
            WHEN state IN ('UNASSIGNED', 'PENDING AUTO ASSIGN', 'UNCONFIGURED') THEN 'Unassigned'
            ELSE NULL
          END AS assignment_state,
          CASE
            WHEN within_sla IS TRUE THEN TRUE
            WHEN within_sla IS FALSE THEN FALSE
            ELSE NULL
          END AS sla_flag,
          0::numeric AS handling_time_days_sum,
          0::bigint AS handling_time_days_count,
          0::numeric AS processing_time_days_sum,
          0::bigint AS processing_time_days_count,
          COUNT(*)::bigint AS task_count
        FROM base
        WHERE due_date IS NOT NULL
          AND (
            state IN ('ASSIGNED', 'UNASSIGNED', 'PENDING AUTO ASSIGN', 'UNCONFIGURED')
            OR LOWER(termination_reason) = 'completed'
          )
        GROUP BY
          1,2,3,4,5,6,7,8,9,10,11,12,13

        UNION ALL

        SELECT
          $1,
          'created'::text AS date_role,
          created_date AS reference_date,
          jurisdiction_label,
          role_category_label,
          region,
          location,
          task_name,
          work_type,
          priority,
          CASE
            WHEN LOWER(termination_reason) = 'completed' THEN 'completed'
            WHEN state IN ('ASSIGNED', 'UNASSIGNED', 'PENDING AUTO ASSIGN', 'UNCONFIGURED') THEN 'open'
            ELSE 'other'
          END AS task_status,
          CASE
            WHEN state = 'ASSIGNED' THEN 'Assigned'
            WHEN state IN ('UNASSIGNED', 'PENDING AUTO ASSIGN', 'UNCONFIGURED') THEN 'Unassigned'
            ELSE NULL
          END AS assignment_state,
          NULL::boolean AS sla_flag,
          0::numeric AS handling_time_days_sum,
          0::bigint AS handling_time_days_count,
          0::numeric AS processing_time_days_sum,
          0::bigint AS processing_time_days_count,
          COUNT(*)::bigint AS task_count
        FROM base
        WHERE created_date IS NOT NULL
        GROUP BY
          1,2,3,4,5,6,7,8,9,10,11,12

        UNION ALL

        SELECT
          $1,
          'completed'::text AS date_role,
          completed_date AS reference_date,
          jurisdiction_label,
          role_category_label,
          region,
          location,
          task_name,
          work_type,
          priority,
          'completed'::text AS task_status,
          NULL::text AS assignment_state,
          CASE
            WHEN within_sla IS TRUE THEN TRUE
            WHEN within_sla IS FALSE THEN FALSE
            ELSE NULL
          END AS sla_flag,
          COALESCE(SUM(EXTRACT(EPOCH FROM handling_time) / EXTRACT(EPOCH FROM INTERVAL '1 day')), 0)::numeric AS handling_time_days_sum,
          COUNT(handling_time)::bigint AS handling_time_days_count,
          COALESCE(SUM(EXTRACT(EPOCH FROM processing_time) / EXTRACT(EPOCH FROM INTERVAL '1 day')), 0)::numeric AS processing_time_days_sum,
          COUNT(processing_time)::bigint AS processing_time_days_count,
          COUNT(*)::bigint AS task_count
        FROM base
        WHERE completed_date IS NOT NULL
          AND LOWER(termination_reason) = 'completed'
        GROUP BY
          1,2,3,4,5,6,7,8,9,10,11,12,13

        UNION ALL

        SELECT
          $1,
          'cancelled'::text AS date_role,
          completed_date AS reference_date,
          jurisdiction_label,
          role_category_label,
          region,
          location,
          task_name,
          work_type,
          priority,
          'cancelled'::text AS task_status,
          NULL::text AS assignment_state,
          NULL::boolean AS sla_flag,
          0::numeric AS handling_time_days_sum,
          0::bigint AS handling_time_days_count,
          0::numeric AS processing_time_days_sum,
          0::bigint AS processing_time_days_count,
          COUNT(*)::bigint AS task_count
        FROM base
        WHERE completed_date IS NOT NULL
          AND termination_reason = 'cancelled'
          AND state IN ('CANCELLED', 'TERMINATED')
        GROUP BY
          1,2,3,4,5,6,7,8,9,10,11,12,13
        $task_daily_insert$,
        v_task_daily_partition_name
      )
      USING v_snapshot_id;

      EXECUTE format(
        'CREATE UNIQUE INDEX %I ON analytics.%I(
           snapshot_id,
           date_role,
           reference_date,
           jurisdiction_label,
           role_category_label,
           region,
           location,
           task_name,
           work_type,
           priority,
           task_status,
           assignment_state,
           sla_flag
         )',
        format('ux_stdf_p_%s_key', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, date_role, task_status, reference_date)',
        format('ix_stdf_p_%s_date_role_status_date', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, reference_date) WHERE date_role = ''due'' AND task_status = ''open''',
        format('ix_stdf_p_%s_due_open_date', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, reference_date, assignment_state) WHERE date_role = ''created'' AND task_status = ''open''',
        format('ix_stdf_p_%s_created_open_date_assignment', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, jurisdiction_label, role_category_label, region, location, task_name, work_type)',
        format('ix_stdf_p_%s_slicers', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, priority)',
        format('ix_stdf_p_%s_priority', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, assignment_state)',
        format('ix_stdf_p_%s_assignment_state', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, sla_flag)',
        format('ix_stdf_p_%s_sla_flag', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'CREATE INDEX %I ON analytics.%I(snapshot_id, UPPER(role_category_label))',
        format('ix_stdf_p_%s_upper_role_category', v_snapshot_id),
        v_task_daily_partition_name
      );

      EXECUTE format(
        'ALTER TABLE analytics.snapshot_task_daily_facts ATTACH PARTITION analytics.%I FOR VALUES IN (%s)',
        v_task_daily_partition_name,
        v_snapshot_id
      );

      EXECUTE format('ANALYZE analytics.%I', v_task_daily_partition_name);

      -- Restore baseline refresh-session settings for subsequent statements.
      PERFORM set_config('enable_sort', v_prev_enable_sort, TRUE);
      PERFORM set_config('work_mem', v_prev_work_mem, TRUE);
      PERFORM set_config('hash_mem_multiplier', v_prev_hash_mem_multiplier, TRUE);

      INSERT INTO analytics.snapshot_wait_time_by_assigned_date (
        snapshot_id,
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        reference_date,
        total_wait_time,
        assigned_task_count
      )
      SELECT
        v_snapshot_id,
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        first_assigned_date AS reference_date,
        SUM(wait_time) AS total_wait_time,
        COUNT(*)::bigint AS assigned_task_count
      FROM analytics.snapshot_task_rows
      WHERE snapshot_id = v_snapshot_id
        AND state = 'ASSIGNED'
        AND wait_time IS NOT NULL
      GROUP BY
        jurisdiction_label,
        role_category_label,
        region,
        location,
        task_name,
        work_type,
        first_assigned_date;
    END IF;

    -- Bias facet aggregation toward in-memory hash aggregate to avoid
    -- external sort spill on larger snapshots.
    v_prev_work_mem := current_setting('work_mem');
    v_prev_hash_mem_multiplier := current_setting('hash_mem_multiplier');
    v_prev_enable_sort := current_setting('enable_sort');

    PERFORM set_config('work_mem', '1GB', TRUE);
    PERFORM set_config('hash_mem_multiplier', '4', TRUE);
    PERFORM set_config('enable_sort', 'off', TRUE);

    CALL analytics.refresh_snapshot_filter_facet_facts(v_snapshot_id);

    -- Restore baseline refresh-session settings for subsequent statements.
    PERFORM set_config('enable_sort', v_prev_enable_sort, TRUE);
    PERFORM set_config('work_mem', v_prev_work_mem, TRUE);
    PERFORM set_config('hash_mem_multiplier', v_prev_hash_mem_multiplier, TRUE);
  EXCEPTION
    WHEN OTHERS THEN
      v_batch_failed := TRUE;
      v_batch_error_message := SQLERRM;
  END;

  IF v_batch_failed THEN
    IF v_task_rows_partition_name IS NOT NULL THEN
      BEGIN
        EXECUTE format('DROP TABLE IF EXISTS analytics.%I', v_task_rows_partition_name);
      EXCEPTION
        WHEN OTHERS THEN
          RAISE WARNING 'Failed to drop task-row partition % after failed batch %: %',
            v_task_rows_partition_name,
            v_snapshot_id,
            SQLERRM;
      END;
    END IF;

    IF v_task_daily_partition_name IS NOT NULL THEN
      BEGIN
        EXECUTE format('DROP TABLE IF EXISTS analytics.%I', v_task_daily_partition_name);
      EXCEPTION
        WHEN OTHERS THEN
          RAISE WARNING 'Failed to drop task-daily partition % after failed batch %: %',
            v_task_daily_partition_name,
            v_snapshot_id,
            SQLERRM;
      END;
    END IF;

    DELETE FROM analytics.snapshot_task_rows WHERE snapshot_id = v_snapshot_id;
    DELETE FROM analytics.snapshot_user_completed_facts WHERE snapshot_id = v_snapshot_id;
    DELETE FROM analytics.snapshot_task_daily_facts WHERE snapshot_id = v_snapshot_id;
    DELETE FROM analytics.snapshot_wait_time_by_assigned_date WHERE snapshot_id = v_snapshot_id;
    DELETE FROM analytics.snapshot_filter_facet_facts WHERE snapshot_id = v_snapshot_id;

    UPDATE analytics.snapshot_batches
    SET status = 'failed', completed_at = clock_timestamp(), error_message = v_batch_error_message
    WHERE snapshot_id = v_snapshot_id;

    UPDATE analytics.snapshot_state
    SET in_progress_snapshot_id = NULL
    WHERE singleton_id = TRUE AND in_progress_snapshot_id = v_snapshot_id;

    COMMIT;
    PERFORM pg_advisory_unlock(v_lock_key);
    RAISE EXCEPTION 'Analytics snapshot batch % failed: %', v_snapshot_id, v_batch_error_message;
  END IF;

  UPDATE analytics.snapshot_batches
  SET status = 'succeeded', completed_at = clock_timestamp(), error_message = NULL
  WHERE snapshot_id = v_snapshot_id;

  UPDATE analytics.snapshot_state
  SET published_snapshot_id = v_snapshot_id,
      published_at = clock_timestamp(),
      in_progress_snapshot_id = NULL
  WHERE singleton_id = TRUE;

  BEGIN
    FOR v_drop_snapshot_id IN
      WITH pinned AS (
        SELECT published_snapshot_id AS snapshot_id
        FROM analytics.snapshot_state
        WHERE singleton_id = TRUE
        UNION
        SELECT in_progress_snapshot_id AS snapshot_id
        FROM analytics.snapshot_state
        WHERE singleton_id = TRUE
      ),
      keep_succeeded AS (
        SELECT snapshot_id
        FROM analytics.snapshot_batches
        WHERE status = 'succeeded'
        ORDER BY snapshot_id DESC
        LIMIT 3
      )
      SELECT batches.snapshot_id
      FROM analytics.snapshot_batches batches
      WHERE batches.status = 'succeeded'
        AND batches.snapshot_id NOT IN (SELECT snapshot_id FROM keep_succeeded)
        AND batches.snapshot_id NOT IN (SELECT snapshot_id FROM pinned WHERE snapshot_id IS NOT NULL)
    LOOP
      EXECUTE format(
        'DROP TABLE IF EXISTS analytics.%I',
        format('snapshot_task_rows_p_%s', v_drop_snapshot_id)
      );
      EXECUTE format(
        'DROP TABLE IF EXISTS analytics.%I',
        format('snapshot_task_daily_facts_p_%s', v_drop_snapshot_id)
      );
      DELETE FROM analytics.snapshot_batches WHERE snapshot_id = v_drop_snapshot_id;
    END LOOP;

    FOR v_drop_snapshot_id IN
      WITH keep_failed AS (
        SELECT snapshot_id
        FROM analytics.snapshot_batches
        WHERE status = 'failed'
        ORDER BY snapshot_id DESC
        LIMIT 100
      )
      SELECT batches.snapshot_id
      FROM analytics.snapshot_batches batches
      WHERE batches.status = 'failed'
        AND batches.snapshot_id NOT IN (SELECT snapshot_id FROM keep_failed)
    LOOP
      EXECUTE format(
        'DROP TABLE IF EXISTS analytics.%I',
        format('snapshot_task_rows_p_%s', v_drop_snapshot_id)
      );
      EXECUTE format(
        'DROP TABLE IF EXISTS analytics.%I',
        format('snapshot_task_daily_facts_p_%s', v_drop_snapshot_id)
      );
      DELETE FROM analytics.snapshot_batches WHERE snapshot_id = v_drop_snapshot_id;
    END LOOP;
  EXCEPTION
    WHEN OTHERS THEN
      RAISE WARNING 'Snapshot retention cleanup failed after publish of %: %', v_snapshot_id, SQLERRM;
  END;

  COMMIT;
  PERFORM pg_advisory_unlock(v_lock_key);
END;
$$;
