/*
 * Set configuration values equivalent to those held in the TaskPerfConfig
 * Java class, if they affect the database.
 */
do $$
begin
	if select current_setting('task_perf_config.use_uniform_role_signatures') is null then
		alter database postgres set task_perf_config.use_uniform_role_signatures to 'N';
	end if;
end;
$$;

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
 * Simple static view for determining which classifications are
 * >= or <= the others.
 */
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

/*
 * Quick and dirty normalisation of task permissions.  Could be more
 * elegant and efficient, but only used during writes, so OK for now.
 * We only need to sear4ch based on read, own, execute and manage, so
 * no need to include any of the others.  (Note: this will change when
 * we add permissions such as claim, etc.)
 */
create view cft_task_db.task_permissions as
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'r' as permission
from cft_task_db.task_roles
where read
union
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'o' as permission
from cft_task_db.task_roles
where own
union
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'x' as permission
from cft_task_db.task_roles
where execute
union
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'm' as permission
from cft_task_db.task_roles
where manage;

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
begin
  if (select current_setting('task_perf_config.use_uniform_role_signatures')) = 'Y' then
    return cft_task_db.uniform_role_signatures(l_task_id);
  else
    return cft_task_db.case_and_org_role_signatures(l_task_id);
  end if;
end;
$$;

create or replace function cft_task_db.case_and_org_role_signatures(l_task_id text)
  returns text[] language plpgsql immutable
as $$
declare
  org_signatures text[];
  case_signatures text[];
begin
    -- The signatures of organisational role assignments which will match this task
    select array_agg(
             'o:' || sig.jurisdiction
             || ':' || sig.region
             || ':' || sig.location
             || ':' || sig.role_name
             || ':' || sig.permission
             || ':' || sig.classification
             || ':' || sig.authorization)
    into org_signatures
    from (
      select
		         t.jurisdiction as jurisdiction,
		         r.region as region,
		         l.location as location,
             p.role_name as role_name,
             p.permission as permission,
             p.authorization as authorization,
             case
                when s.higher = 'PUBLIC' then 'U'
                when s.higher = 'PRIVATE' then 'P'
                when s.higher = 'RESTRICTED' then 'R'
                else s.higher end as classification
      from   cft_task_db.tasks t,
             cft_task_db.task_permissions p,
             cft_task_db.classifications s,
             (select unnest(array['*', region]) as region
              from cft_task_db.tasks where task_id = l_task_id) r,
             (select unnest(array['*', location]) as location
              from cft_task_db.tasks where task_id = l_task_id) l
      where  t.task_id = l_task_id
      and    p.task_id = l_task_id
      and    s.lower = t.security_classification::text) sig;
    -- The signatures of role assignments which will match this task
    select array_agg(
             'c:'
             || ':' || sig.case_id
             || ':' || sig.role_name
             || ':' || sig.permission
             || ':' || sig.classification
             || ':' || sig.authorization)
    into case_signatures
    from (
      select
		         t.case_id as case_id,
             p.role_name as role_name,
             p.permission as permission,
             p.authorization as authorization,
             case
                when s.higher = 'PUBLIC' then 'U'
                when s.higher = 'PRIVATE' then 'P'
                when s.higher = 'RESTRICTED' then 'R'
                else s.higher end as classification
      from   cft_task_db.tasks t,
             cft_task_db.task_permissions p,
             cft_task_db.classifications s
      where  t.task_id = l_task_id
      and    p.task_id = l_task_id
      and    s.lower = t.security_classification::text) sig;
    return org_signatures || case_signatures;
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

create or replace function cft_task_db.uniform_role_signatures(l_task_id text)
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
			permissions (role_name, "permission", "authorization") as (
				select role_name, "permission", "authorization"
				from cft_task_db.task_permissions
				where task_id = l_task_id)
		select
			j.jurisdiction as jurisdiction,
			r.region as region,
			l.location as location,
			i.case_id as case_id,
			p.role_name as role_name,
			p.permission as "permission",
			p.authorization as "authorization",
			c.classification as classification
		from
			jurisdictions j,
			regions r,
			locations l,
			case_ids i,
			permissions p,
			classifications c) sig;
    return l_signatures;
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
begin
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
      select
             case
                when s.state = 'ASSIGNED' then 'A'
                when s.state = 'UNASSIGNED' then 'U'
                else s.state end as state,
             j.jurisdiction as jurisdiction,
             case
                when c.role_category = 'JUDICIAL' then 'J'
                when c.role_category = 'LEGAL_OPERATIONS' then 'L'
                when c.role_category = 'ADMIN' then 'A'
                when c.role_category = 'UNKNOWN' then 'U'
                else c.role_category end as role_category,
             w.work_type as work_type,
             r.region as region,
             l.location as location
      from   cft_task_db.tasks t,
             (select unnest(cft_task_db.add_wildcard(jurisdiction)) as jurisdiction
              from cft_task_db.tasks where task_id = l_task_id) j,
             (select unnest(cft_task_db.add_wildcard(state::text)) as state
              from cft_task_db.tasks where task_id = l_task_id) s,
             (select unnest(cft_task_db.add_wildcard(role_category::text)) as role_category
              from cft_task_db.tasks where task_id = l_task_id) c,
             (select unnest(cft_task_db.add_wildcard(work_type)) as work_type
              from cft_task_db.tasks where task_id = l_task_id) w,
             (select unnest(cft_task_db.add_wildcard(region)) as region
              from cft_task_db.tasks where task_id = l_task_id) r,
             (select unnest(cft_task_db.add_wildcard(location)) as location
              from cft_task_db.tasks where task_id = l_task_id) l
      where  t.task_id = l_task_id) sig;
    return l_signatures;
end;
$$;

/*
 * Needed to add scalar values to the GIN index.
 */
create extension if not exists btree_gin;

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