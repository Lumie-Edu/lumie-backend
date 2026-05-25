-- Provision the `langgraph` schema for the LangGraph AsyncPostgresSaver checkpointer.
-- The checkpointer's .setup() call creates its own tables (checkpoints,
-- checkpoint_blobs, checkpoint_writes, checkpoint_migrations) at service boot;
-- this migration only creates the schema and sets up the dedicated role.
--
-- Role chicken-and-egg rationale:
--   In real clusters (dev cluster, prod) `lumie_langgraph` is created with LOGIN +
--   password by a Vault-templated CNPG postInit SQL hook before Flyway runs, so the
--   DO block below is a no-op there.  In local dev (plain postgres, no Vault) the
--   role does not exist yet, so the GRANT statements would fail without it.  We
--   create it here as NOLOGIN so that grants resolve; Vault/postInit layers on the
--   LOGIN + password out-of-band in real clusters.
--
-- Isolation: lumie_langgraph is granted NOTHING on the public schema (tenant data).
-- The public-schema boundary is enforced by the ABSENCE of grants on public — not by
-- search_path alone — so it holds even if a session resets its search_path.
-- Tenant isolation within the langgraph schema is enforced via thread_id namespacing
-- (dedicated role + schema boundary; no RLS needed — no tenant_id column exists here).

-- 1. Schema
CREATE SCHEMA IF NOT EXISTS langgraph;

-- 2. Role (idempotent; no-op if already created by CNPG postInit)
DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lumie_langgraph') THEN
    CREATE ROLE lumie_langgraph NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  END IF;
END $$;

-- 3. Schema-level grants
--    CREATE is required so AsyncPostgresSaver.setup() can create its checkpoint tables.
GRANT USAGE, CREATE ON SCHEMA langgraph TO lumie_langgraph;

-- 4. Object-level grants on anything already in the schema (safe to run even when empty)
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA langgraph TO lumie_langgraph;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA langgraph TO lumie_langgraph;

-- 5. Default privileges so objects created later are automatically accessible.
--    setup() connects as lumie_langgraph and owns the tables it creates, so this is a
--    belt-and-suspenders clause covering any objects postgres might create here later.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA langgraph
  GRANT ALL ON TABLES    TO lumie_langgraph;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA langgraph
  GRANT ALL ON SEQUENCES TO lumie_langgraph;

-- 6. Pin the role's default search_path so AsyncPostgresSaver CREATE TABLEs land
--    in langgraph rather than public.
ALTER ROLE lumie_langgraph SET search_path = langgraph;
