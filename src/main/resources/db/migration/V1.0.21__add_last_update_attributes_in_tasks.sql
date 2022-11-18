ALTER TABLE tasks ADD COLUMN last_updated_timestamp TIMESTAMP;
ALTER TABLE tasks ADD COLUMN last_updated_user TEXT;
ALTER TABLE tasks ADD COLUMN last_updated_action TEXT;
