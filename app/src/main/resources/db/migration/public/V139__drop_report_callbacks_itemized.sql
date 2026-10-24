set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- All new callbacks use report_generation_items. No in-progress legacy report
-- jobs remain, so the compatibility discriminator is no longer needed.
-- Deploy only after every application instance that reads callbacks_itemized
-- has stopped; older instances are not compatible with this contract migration.
alter table report_generation_jobs
    drop column if exists callbacks_itemized;
