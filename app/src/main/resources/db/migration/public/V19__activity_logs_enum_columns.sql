-- Convert activity_logs.actor_role / action / entity_type from VARCHAR to
-- native Postgres ENUM types for type safety + storage compactness.
--
-- Risk: ALTER COLUMN ... TYPE ... USING <col>::<type> fails if any row holds
-- a value not declared in the corresponding ENUM. Pre-flight check below
-- aborts the migration with a clear error before the destructive ALTER.
--
-- To extend an ENUM later: ALTER TYPE <name> ADD VALUE 'NEW_VALUE';
-- (Cannot be inside a transaction. Run via a separate Flyway migration.)

-- Pre-flight: validate no row contains unknown values.
DO $$
DECLARE
    bad_actions text[];
    bad_entities text[];
    bad_roles text[];
BEGIN
    SELECT array_agg(DISTINCT action ORDER BY action) INTO bad_actions
    FROM public.activity_logs
    WHERE action NOT IN (
        'VIEW', 'CREATE', 'UPDATE', 'DELETE', 'DEACTIVATE', 'REACTIVATE',
        'TERMINATE', 'ENROLL', 'DROP', 'SEND', 'GRADE', 'CHECK_IN',
        'CLOSE', 'RESET_PASSWORD', 'CHANGE_LOGIN_ID'
    );
    IF bad_actions IS NOT NULL AND array_length(bad_actions, 1) > 0 THEN
        RAISE EXCEPTION 'activity_logs.action contains unknown values: %. Update the ENUM or clean the data before migrating.', bad_actions;
    END IF;

    SELECT array_agg(DISTINCT entity_type ORDER BY entity_type) INTO bad_entities
    FROM public.activity_logs
    WHERE entity_type NOT IN (
        'CLASS', 'STUDENT', 'ADMIN', 'ATTENDANCE', 'ASSIGNMENT', 'EXAM',
        'EXAM_TEMPLATE', 'LECTURE', 'ANNOUNCEMENT', 'SMS', 'RESOURCE',
        'REVIEW', 'TEXTBOOK', 'ACADEMY', 'SCHEDULE', 'PERMISSION',
        'ENROLLMENT', 'RESERVATION', 'QNA', 'CONVERSATION', 'CHAT', 'FILE',
        'SCHEDULED_TASK', 'STATISTICS', 'REPORT', 'RESULT'
    );
    IF bad_entities IS NOT NULL AND array_length(bad_entities, 1) > 0 THEN
        RAISE EXCEPTION 'activity_logs.entity_type contains unknown values: %. Update the ENUM or clean the data before migrating.', bad_entities;
    END IF;

    SELECT array_agg(DISTINCT actor_role ORDER BY actor_role) INTO bad_roles
    FROM public.activity_logs
    WHERE actor_role NOT IN ('OWNER', 'MANAGER', 'INSTRUCTOR', 'STUDENT');
    IF bad_roles IS NOT NULL AND array_length(bad_roles, 1) > 0 THEN
        RAISE EXCEPTION 'activity_logs.actor_role contains unknown values: %. Map legacy aliases (ADMIN → INSTRUCTOR, DEVELOPER → MANAGER) before migrating.', bad_roles;
    END IF;
END $$;

-- Create native ENUM types. Names are prefixed to avoid clash with the
-- generic words "action" / "entity_type" / "role" in the global namespace.
CREATE TYPE activity_action_type AS ENUM (
    'VIEW', 'CREATE', 'UPDATE', 'DELETE', 'DEACTIVATE', 'REACTIVATE',
    'TERMINATE', 'ENROLL', 'DROP', 'SEND', 'GRADE', 'CHECK_IN',
    'CLOSE', 'RESET_PASSWORD', 'CHANGE_LOGIN_ID'
);

CREATE TYPE activity_entity_type AS ENUM (
    'CLASS', 'STUDENT', 'ADMIN', 'ATTENDANCE', 'ASSIGNMENT', 'EXAM',
    'EXAM_TEMPLATE', 'LECTURE', 'ANNOUNCEMENT', 'SMS', 'RESOURCE',
    'REVIEW', 'TEXTBOOK', 'ACADEMY', 'SCHEDULE', 'PERMISSION',
    'ENROLLMENT', 'RESERVATION', 'QNA', 'CONVERSATION', 'CHAT', 'FILE',
    'SCHEDULED_TASK', 'STATISTICS', 'REPORT', 'RESULT'
);

CREATE TYPE actor_role_type AS ENUM ('OWNER', 'MANAGER', 'INSTRUCTOR', 'STUDENT');

-- Convert columns to ENUM types using the explicit string cast.
ALTER TABLE public.activity_logs
    ALTER COLUMN action TYPE activity_action_type USING action::activity_action_type,
    ALTER COLUMN entity_type TYPE activity_entity_type USING entity_type::activity_entity_type,
    ALTER COLUMN actor_role TYPE actor_role_type USING actor_role::actor_role_type;
