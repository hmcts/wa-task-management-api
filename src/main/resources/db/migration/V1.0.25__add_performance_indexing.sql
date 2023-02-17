/*
 * Set configuration values equivalent to those held in the TaskPerfConfig
 * Java class, if they affect the database.
 */
-- alter database cft_task_db set task_perf_config.use_uniform_role_signatures to 'Y';

/*
 * Column to avoid index churn.  We don't want to modify indexes until the
 * task and all its permissions have been modified - the signature-generating
 * functions are lying about being immutable, but that's OK as long as we
 * set indexed = false before making any changes, make all changes to a task
 * and its permissions, then set indexed = true again.  The task will be
 * removed from the index in the first step, and added again by the last.
 */
alter table cft_task_db.tasks add column indexed boolean not null default false;

/*
 * Needed to add scalar values to the GIN index.
 */
create extension if not exists btree_gin;

/*
 * Simple static view for determining which classifications are
 * >= or <= the others.
 */
drop view if exists cft_task_db.classifications;

create view cft_task_db.classifications as
with classifications (lower, higher) as (
    values
        ('PUBLIC','PUBLIC'),
        ('PUBLIC','PRIVATE'),
        ('PUBLIC','RESTRICTED'),
        ('PRIVATE','PRIVATE'),
        ('PRIVATE','RESTRICTED'),
        ('RESTRICTED','RESTRICTED'))
select lower,higher from classifications;

-- Update task_permissions view

drop view if exists cft_task_db.task_permissions;

create view cft_task_db.task_permissions as
-- Read permission ignores authorisations
select task_id as task_id,
       role_name as role_name,
       '*' as authorization,
       'r' as permission
from cft_task_db.task_roles
where read
union
-- Manage permission ignores authorisations
select task_id as task_id,
       role_name as role_name,
       '*' as authorization,
       'm' as permission
from cft_task_db.task_roles
where manage
union
-- Available permission handles authorisations
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'a' as permission
from cft_task_db.task_roles
where own and claim;


/*
 * Calculates the signatures of all role assignments which would match
 * the permissions on this task.  Each signature is a unique text string
 * which captures the role assignment attributes, the permision configured
 * for the relevant role, the authorisations required for the permission.
 *
 * Since some role assignment attributes are optional, there are signatures
 * with all possible combinations of the attributes being present or not.
 * There are 5 optional attributes, making for 32 signature variants for
 * each task/permission.
 */

create or replace function cft_task_db.role_signatures(l_task_id text)
  returns text[] language plpgsql immutable
as $$
declare
l_signatures text[];
  l_task_data record;
begin
	-- Get the task data needed to generate signatures
select jurisdiction, region, location, case_id, security_classification::text
into l_task_data
from cft_task_db.tasks
where task_id = l_task_id;
-- Generate all the role assignment signatures which will match this task
select array_agg(
                   sig.jurisdiction
                   || ':' || sig.region
                   || ':' || sig.location
                   || ':' || sig.role_name
                   || ':' || sig.case_id
                   || ':' || sig.permission
                   || ':' || sig.classification
                   || ':' || sig.authorization)
into l_signatures
from (
         with
             jurisdictions (jurisdiction) as (
                 select unnest(cft_task_db.add_wildcard(l_task_data.jurisdiction))),
             regions (region) as (
                 select unnest(cft_task_db.add_wildcard(l_task_data.region))),
             locations (location) as (
                 select unnest(cft_task_db.add_wildcard(l_task_data.location))),
             case_ids (case_id) as (
                 select unnest(cft_task_db.add_wildcard(l_task_data.case_id))),
             classifications (classification) as (
                 select cft_task_db.abbreviate_classification(higher)
                 from cft_task_db.classifications
                 where lower = l_task_data.security_classification),
             -- Org role permissions use authorizations.  Note that authorisations
             -- are only present in the view for the "a" (available) permission.
             org_role_permissions (role_name, "permission", "authorization") as (
                 select distinct role_name, "permission", "authorization"
                 from cft_task_db.task_permissions
                 where task_id = l_task_id),
             -- Case role permissions do not use authorisations (=> wildcard)
             case_role_permissions (role_name, "permission", "authorization") as (
                 select distinct role_name, "permission", '*' as authorization
         from cft_task_db.task_permissions
         where task_id = l_task_id)
-- Org role permissions
select
    j.jurisdiction as jurisdiction,
    r.region as region,
    l.location as location,
    -- Org role assignments do not have a case ID
    '*' as case_id,
    p.role_name as role_name,
    p.permission as "permission",
    p.authorization as "authorization",
    c.classification as classification
from
    jurisdictions j,
    regions r,
    locations l,
    case_ids i,
    -- Use the permissions data for org roles (has authorisations).
    org_role_permissions p,
    classifications c
union all
-- Case role permissions
select
    j.jurisdiction as jurisdiction,
    r.region as region,
    l.location as location,
    -- Case roles have the case ID specified (no wildcard)
    l_task_data.case_id as case_id,
    p.role_name as role_name,
    p.permission as "permission",
    p.authorization as "authorization",
    c.classification as classification
from
    jurisdictions j,
    regions r,
    locations l,
    case_ids i,
    -- Use the permissions data for case roles (no authorisations).
    case_role_permissions p,
    classifications c) sig;
return l_signatures;
end;
$$;

/*
 * Returns an array containing a '*' wildcard and the given value, if it is not null.
 */
create or replace function cft_task_db.add_wildcard(l_value text)
  returns text[] language plpgsql immutable
as $$
declare
l_values text[] := ARRAY['*'];
begin
	if l_value is not null then
		l_values := array_append(l_values, l_value);
end if;
return l_values;
end;
$$;

/*
 * Returns the abbreviation for the given security classification.
 */
create or replace function cft_task_db.abbreviate_classification(l_classification text)
	returns text language plpgsql immutable
as $$
begin
return
    case
        when l_classification = 'PUBLIC' then 'U'
        when l_classification = 'PRIVATE' then 'P'
        when l_classification = 'RESTRICTED' then 'R'
        else l_classification
        end;
end;
$$;

/*
 * Returns the abbreviation for the given state.
 */
create or replace function cft_task_db.abbreviate_state(l_state text)
	returns text language plpgsql immutable
as $$
begin
return
    case
        when l_state = 'UNASSIGNED' then 'U'
        when l_state = 'ASSIGNED' then 'A'
        else null
        end;
end;
$$;

/*
 * Returns the abbreviation for the given role category.
 */
create or replace function cft_task_db.abbreviate_role_category(l_role_category text)
	returns text language plpgsql immutable
as $$
begin
return
    case
        when l_role_category = 'JUDICIAL' then 'J'
        when l_role_category = 'LEGAL_OPERATIONS' then 'L'
        when l_role_category = 'ADMIN' then 'A'
        when l_role_category = 'CTSC' then 'C'
        else null
        end;
end;
$$;


/*
 * Calculates the signatures of all combined filter conditions which
 * would match the task.  Each signature is a unique text string which
 * encodes the filter.  Wildcards are reprsented with '*'.
 */
create or replace function cft_task_db.filter_signatures(l_task_id text)
  returns text[] language plpgsql immutable
as $$
declare
l_signatures text[];
  l_task_data record;
begin
	-- Get the task data needed to generate signatures
select state::text, jurisdiction, role_category, work_type,region, location
into l_task_data
from cft_task_db.tasks
where task_id = l_task_id;
-- The signatures of client filters which will match this task
select array_agg(
                   sig.state
                   || ':' || sig.jurisdiction
                   || ':' || sig.role_category
                   || ':' || sig.work_type
                   || ':' || sig.region
                   || ':' || sig.location)
into l_signatures
from (
         with
             states (state) as (select unnest(cft_task_db.add_wildcard(cft_task_db.abbreviate_state(l_task_data.state)))),
             jurisdictions (jurisdiction) as (select unnest(cft_task_db.add_wildcard(l_task_data.jurisdiction))),
             role_categories (role_category) as(select unnest(cft_task_db.add_wildcard(cft_task_db.abbreviate_role_category(l_task_data.role_category)))),
             work_types (work_type) as (select unnest(cft_task_db.add_wildcard(l_task_data.work_type))),
             regions (region) as (select unnest(cft_task_db.add_wildcard(l_task_data.region))),
             locations (location) as (select unnest(cft_task_db.add_wildcard(l_task_data.location)))
         select
             s.state as state,
             j.jurisdiction as jurisdiction,
             c.role_category as role_category,
             w.work_type as work_type,
             r.region as region,
             l.location as location
         from
             states s,
             jurisdictions j,
             role_categories c,
             work_types w,
             regions r,
             locations l) sig;
return l_signatures;
end;
$$;

/*
 * GIN index to look up tasks based on role signatures and filter signatures.
 * Only covers assigned and unassigned tasks which have been flagged as
 * indexed.  There is no value in indexing old tasks, since we never search
 * them.
 */
drop index if exists cft_task_db.search_index;
create index search_index on cft_task_db.tasks using gin (
    cft_task_db.filter_signatures(task_id),
    cft_task_db.role_signatures(task_id),
    case_id,
    assignee)
    where state in ('ASSIGNED','UNASSIGNED') and indexed;


/*
 * Reindex all tasks.
 */
create or replace procedure cft_task_db.reindex_all_tasks()
	language plpgsql
as $$
begin
update cft_task_db.tasks set indexed = false;
commit;
update cft_task_db.tasks set indexed = true where state in ('ASSIGNED'::cft_task_db.task_state_enum, 'UNASSIGNED'::cft_task_db.task_state_enum);
commit;
end;
$$;

/*
 * To reindex all tasks, run:
 * call cft_task_db.reindex_all_tasks();
 */
