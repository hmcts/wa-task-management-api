/*
 * Replace the legacy GIN expression index with materialised signature arrays.
 *
 * Exact text signatures remain the source of truth. Compact integer hashes are
 * indexed with GiST to find candidates quickly, then the query checks the exact
 * arrays to guard against hash collisions.
 */

CREATE EXTENSION IF NOT EXISTS intarray;

ALTER TABLE cft_task_db.tasks
    ADD COLUMN filter_signatures TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN role_signatures TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN filter_signature_hashes INTEGER[] NOT NULL DEFAULT '{}',
    ADD COLUMN role_signature_hashes INTEGER[] NOT NULL DEFAULT '{}';

CREATE OR REPLACE FUNCTION cft_task_db.canonical_signatures(l_signatures TEXT[])
    RETURNS TEXT[] LANGUAGE sql IMMUTABLE
AS $$
SELECT COALESCE(ARRAY_AGG(DISTINCT signature ORDER BY signature), '{}'::TEXT[])
FROM UNNEST(COALESCE(l_signatures, '{}'::TEXT[])) AS signatures(signature);
$$;

CREATE OR REPLACE FUNCTION cft_task_db.signature_hashes(l_signatures TEXT[])
    RETURNS INTEGER[] LANGUAGE sql IMMUTABLE
AS $$
SELECT COALESCE(ARRAY_AGG(DISTINCT hashtext(signature) ORDER BY hashtext(signature)), '{}'::INTEGER[])
FROM UNNEST(COALESCE(l_signatures, '{}'::TEXT[])) AS signatures(signature);
$$;

/*
 * role_signatures reads task_roles through task_permissions. It no longer needs
 * to pretend to be immutable now that it is not used in an expression index.
 */
ALTER FUNCTION cft_task_db.role_signatures(
    TEXT,
    TEXT,
    TEXT,
    TEXT,
    TEXT,
    cft_task_db.security_classification_enum
) STABLE;

CREATE OR REPLACE FUNCTION cft_task_db.materialise_filter_signatures(
    l_task_id TEXT,
    l_state cft_task_db.task_state_enum,
    l_jurisdiction TEXT,
    l_role_category TEXT,
    l_work_type TEXT,
    l_region TEXT,
    l_location TEXT
)
    RETURNS TEXT[] LANGUAGE sql IMMUTABLE
AS $$
SELECT cft_task_db.canonical_signatures(
    cft_task_db.filter_signatures(
        l_task_id,
        l_state,
        l_jurisdiction,
        l_role_category,
        l_work_type,
        l_region,
        l_location
    )
);
$$;

CREATE OR REPLACE FUNCTION cft_task_db.materialise_role_signatures(
    l_task_id TEXT,
    l_jurisdiction TEXT,
    l_region TEXT,
    l_location TEXT,
    l_case_id TEXT,
    l_security_classification cft_task_db.security_classification_enum
)
    RETURNS TEXT[] LANGUAGE sql STABLE
AS $$
SELECT cft_task_db.canonical_signatures(
    cft_task_db.role_signatures(
        l_task_id,
        l_jurisdiction,
        l_region,
        l_location,
        l_case_id,
        l_security_classification
    )
);
$$;

CREATE OR REPLACE FUNCTION cft_task_db.refresh_task_search_columns()
    RETURNS TRIGGER LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.indexed THEN
        NEW.filter_signatures := cft_task_db.materialise_filter_signatures(
            NEW.task_id,
            NEW.state,
            NEW.jurisdiction,
            NEW.role_category,
            NEW.work_type,
            NEW.region,
            NEW.location
        );
        NEW.role_signatures := cft_task_db.materialise_role_signatures(
            NEW.task_id,
            NEW.jurisdiction,
            NEW.region,
            NEW.location,
            NEW.case_id,
            NEW.security_classification
        );
        NEW.filter_signature_hashes := cft_task_db.signature_hashes(NEW.filter_signatures);
        NEW.role_signature_hashes := cft_task_db.signature_hashes(NEW.role_signatures);
    ELSE
        NEW.filter_signatures := '{}'::TEXT[];
        NEW.role_signatures := '{}'::TEXT[];
        NEW.filter_signature_hashes := '{}'::INTEGER[];
        NEW.role_signature_hashes := '{}'::INTEGER[];
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER refresh_task_search_columns_on_tasks
    BEFORE INSERT OR UPDATE OF indexed, state, jurisdiction, role_category, work_type, region, location, case_id,
        security_classification
    ON cft_task_db.tasks
    FOR EACH ROW
    EXECUTE FUNCTION cft_task_db.refresh_task_search_columns();

/*
 * A task_roles change must refresh the materialised parent RBAC signatures in
 * the same transaction. Updates to unrelated granular permissions do not churn
 * the search indexes.
 */
CREATE OR REPLACE FUNCTION cft_task_db.refresh_task_role_search_columns()
    RETURNS TRIGGER LANGUAGE plpgsql
AS $$
DECLARE
    l_task_ids TEXT[];
    l_task_id TEXT;
    l_role_signatures TEXT[];
BEGIN
    IF TG_OP = 'INSERT' THEN
        l_task_ids := ARRAY[NEW.task_id];
    ELSIF TG_OP = 'DELETE' THEN
        l_task_ids := ARRAY[OLD.task_id];
    ELSIF OLD.task_id IS DISTINCT FROM NEW.task_id THEN
        l_task_ids := ARRAY[OLD.task_id, NEW.task_id];
    ELSE
        l_task_ids := ARRAY[NEW.task_id];
    END IF;

    FOREACH l_task_id IN ARRAY l_task_ids LOOP
        CONTINUE WHEN l_task_id IS NULL;

        SELECT cft_task_db.materialise_role_signatures(
            task_id,
            jurisdiction,
            region,
            location,
            case_id,
            security_classification
        )
        INTO l_role_signatures
        FROM cft_task_db.tasks
        WHERE task_id = l_task_id
          AND indexed;

        IF FOUND THEN
            UPDATE cft_task_db.tasks
            SET role_signatures = l_role_signatures,
                role_signature_hashes = cft_task_db.signature_hashes(l_role_signatures)
            WHERE task_id = l_task_id;
        END IF;
    END LOOP;

    RETURN NULL;
END;
$$;

CREATE TRIGGER refresh_task_search_columns_on_task_roles
    AFTER INSERT OR DELETE OR UPDATE OF task_id, role_name, read, manage, own, claim, authorizations
    ON cft_task_db.task_roles
    FOR EACH ROW
    EXECUTE FUNCTION cft_task_db.refresh_task_role_search_columns();

UPDATE cft_task_db.tasks task
SET filter_signatures = signatures.filter_signatures,
    role_signatures = signatures.role_signatures,
    filter_signature_hashes = cft_task_db.signature_hashes(signatures.filter_signatures),
    role_signature_hashes = cft_task_db.signature_hashes(signatures.role_signatures)
FROM (
    SELECT task_id,
           cft_task_db.materialise_filter_signatures(
               task_id,
               state,
               jurisdiction,
               role_category,
               work_type,
               region,
               location
           ) AS filter_signatures,
           cft_task_db.materialise_role_signatures(
               task_id,
               jurisdiction,
               region,
               location,
               case_id,
               security_classification
           ) AS role_signatures
    FROM cft_task_db.tasks
    WHERE indexed
) signatures
WHERE task.task_id = signatures.task_id;
