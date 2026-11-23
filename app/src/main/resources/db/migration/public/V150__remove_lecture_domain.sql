-- The lecture feature has been removed from the application.
-- This is intentionally irreversible: stored lecture records and their class
-- targeting data are no longer part of the product or retained data model.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

DROP TABLE IF EXISTS lecture_class_targets;
DROP TABLE IF EXISTS lectures;
