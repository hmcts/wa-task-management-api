-- tasks indexes

CREATE INDEX idx_t_task_id ON cft_task_db.tasks (state);

-- task_history indexes

CREATE INDEX idx_th_created_update_id ON cft_task_db.task_history (created, update_id);
CREATE INDEX idx_th_updated_update_id ON cft_task_db.task_history (updated, update_id);

-- reportable_task indexes

CREATE INDEX idx_rt_created_date_update_id ON cft_task_db.reportable_task (created_date, update_id);
CREATE INDEX idx_rt_due_date_update_id ON cft_task_db.reportable_task (due_date, update_id);
CREATE INDEX idx_rt_state_rep ON cft_task_db.reportable_task (state);
CREATE INDEX idx_rt_final_state_label ON cft_task_db.reportable_task (final_state_label);
CREATE INDEX idx_is_within_sla ON cft_task_db.reportable_task (is_within_sla);
CREATE INDEX idx_state_termination_reason ON cft_task_db.reportable_task (state, termination_reason);
CREATE INDEX idx_completed_date_time ON cft_task_db.reportable_task (completed_date_time);
CREATE INDEX idx_completed_date ON cft_task_db.reportable_task (completed_date);
CREATE INDEX idx_wait_time_days ON cft_task_db.reportable_task (wait_time_days);
CREATE INDEX idx_wait_time ON cft_task_db.reportable_task (wait_time);
CREATE INDEX idx_handling_time ON cft_task_db.reportable_task (handling_time);
CREATE INDEX idx_processing_time ON cft_task_db.reportable_task (processing_time);
CREATE INDEX idx_first_assigned_date ON cft_task_db.reportable_task (first_assigned_date);
CREATE INDEX idx_first_assigned_date_time ON cft_task_db.reportable_task (first_assigned_date_time);

-- task_assignments indexes

CREATE INDEX idx_ta_task_id ON cft_task_db.task_assignments (task_id);
CREATE INDEX idx_ta_assignment_start ON cft_task_db.task_assignments (assignment_start);
CREATE INDEX idx_ta_assignment_end ON cft_task_db.task_assignments (assignment_end);
