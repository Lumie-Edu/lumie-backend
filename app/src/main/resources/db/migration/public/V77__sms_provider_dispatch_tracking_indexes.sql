-- V76 already creates the SMS provider dispatch indexes.
-- Keep V77 as a transactional no-op so deployments that have not applied it yet
-- can advance without running PostgreSQL CONCURRENTLY statements through Flyway.
select 1;
