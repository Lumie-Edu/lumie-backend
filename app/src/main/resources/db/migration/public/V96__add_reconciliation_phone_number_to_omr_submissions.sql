-- =====================================================================
-- V96: add principal-entered reconciliation phone number to OMR submissions
--
-- Preserve the OCR-captured phone number as immutable source data and store
-- any principal-entered reconciliation phone separately. Existing rows are
-- intentionally not backfilled.
-- =====================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists omr_submissions
    add column if not exists reconciliation_phone_number varchar(20);
