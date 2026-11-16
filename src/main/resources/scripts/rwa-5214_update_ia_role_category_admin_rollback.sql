-- 1. Verify the backup has the expected 37 rows
select count(*) from cft_task_db.rwa_role_category_admin_bakcup;

-- Update IA role categories
update cft_task_db.tasks t
set role_category = b.role_category
from cft_task_db.rwa_role_category_admin_bakcup b
where t.task_id = b.task_id;


-- Drop the temp backup table if not needed
drop table if exists cft_task_db.rwa_role_category_admin_bakcup
