-- ShedLock distributed-lock table for @SchedulerLock-annotated cron jobs.
-- Lives in the public schema (cross-tenant) since schedulers iterate all tenants.

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
