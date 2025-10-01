--Connect to main database
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- 8

update cft_task_db.tasks set termination_reason = 'https://tools.hmcts.net/jira/browse/RWA-4667' where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- Updated Rows = 8

-- Verify the update
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- 0


