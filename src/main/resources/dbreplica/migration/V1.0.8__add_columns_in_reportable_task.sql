ALTER TABLE reportable_task ADD COLUMN state_label TEXT NOT NULL;
ALTER TABLE reportable_task ADD COLUMN role_category_label TEXT;
ALTER TABLE reportable_task ADD COLUMN jurisdiction_label TEXT NOT NULL;
ALTER TABLE reportable_task ADD COLUMN case_type_label TEXT NOT NULL;