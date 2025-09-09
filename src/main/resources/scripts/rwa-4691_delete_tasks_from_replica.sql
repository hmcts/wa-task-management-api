--Connect to primary database and run below query to make sure task does not exist in primary database
select count(*) from cft_task_db.cft_task_db.tasks t  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--    0|

--Connect to replica database and run below queries
select count(*) from cft_task_db.cft_task_db.tasks t  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--    0|

select count(*) from cft_task_db.cft_task_db.reportable_task rt  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--   16|

select count(*) from cft_task_db.cft_task_db.task_history th where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--  108|

select count(*) from cft_task_db.cft_task_db.task_assignments ta where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--  23|

--Create backup tables and take backup of data before deleting
create table IF NOT EXISTS cft_task_db.cft_task_db.deleted_tasks_reportable_task_backup AS
select * from cft_task_db.cft_task_db.reportable_task rt  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');


create table IF NOT EXISTS cft_task_db.cft_task_db.deleted_tasks_task_history_backup AS
select * from cft_task_db.cft_task_db.task_history th  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');

create table IF NOT EXISTS cft_task_db.cft_task_db.deleted_tasks_task_assignments_backup AS
select * from cft_task_db.cft_task_db.task_assignments ta  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');

-- Verify backup data
select count(*) from cft_task_db.cft_task_db.deleted_tasks_reportable_task_backup;
--count|
-------+
--   16|

select count(*) from cft_task_db.cft_task_db.deleted_tasks_task_history_backup;
--count|
-------+
--  108|

select count(*) from cft_task_db.cft_task_db.deleted_tasks_task_assignments_backup;
--count|
-------+
--  23|


--delete task data from replica database

delete from cft_task_db.cft_task_db.reportable_task where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f') and jurisdiction='CIVIL';

delete from cft_task_db.cft_task_db.task_history th  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f') and jurisdiction='CIVIL';

delete from cft_task_db.cft_task_db.task_assignments ta where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f') and jurisdiction='CIVIL';

-- Verify data deletion

select count(*) from cft_task_db.cft_task_db.reportable_task rt  where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--    0|

select count(*) from cft_task_db.cft_task_db.task_history th where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--    0|

select count(*) from cft_task_db.cft_task_db.task_assignments ta where task_id in ('2824a694-e2f5-11ef-9b30-cec93d3a1e54','5c44796c-dfe9-11ef-9909-decc539c5471','66d24e7a-e569-11ef-b314-be1ef270789f','6c0595b1-e3ac-11ef-ab2f-061ac63dbba8','6e12ce1b-e2ec-11ef-9909-decc539c5471','a531940a-e2f1-11ef-b314-be1ef270789f','a887da41-e465-11ef-b314-be1ef270789f','c3074437-df22-11ef-9998-82aee8ed8d10','c752dcad-e48e-11ef-9b30-cec93d3a1e54','c7ce8a6b-deea-11ef-9bdb-beb3c1964479','d76afde3-e567-11ef-9bdb-beb3c1964479','e0a17fbf-e394-11ef-b314-be1ef270789f','f0dd4b4f-e220-11ef-9b30-cec93d3a1e54','f11b1ac1-e224-11ef-aa7b-a2aa7df8165d','f5f6751d-e479-11ef-ab2f-061ac63dbba8','fe5b4b3b-e568-11ef-b314-be1ef270789f');
--count|
-------+
--    0|

-- Delete backup tables that were created
drop table cft_task_db.cft_task_db.deleted_tasks_reportable_task_backup;
drop table cft_task_db.cft_task_db.deleted_tasks_task_history_backup;
drop table cft_task_db.cft_task_db.deleted_tasks_task_assignments_backup;
-- End of script




