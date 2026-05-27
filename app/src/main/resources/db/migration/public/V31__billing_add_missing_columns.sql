-- =====================================================================
-- V31: Add columns missing from V28 billing schema
--
-- B2: subscriptions.payment_failure_count
--   Tracks consecutive auto-charge failures per subscription aggregate.
--   Resets to 0 on successful charge or manual resume.
--   The Subscription domain entity auto-pauses at 3 consecutive failures.
--
-- B3: tax_invoices.failure_reason
--   Stores the error message returned by Popbill / PG when issue_status
--   transitions to 'FAILED'. VARCHAR(500) matches entity @Column(length=500).
--
-- N2: Update payment_transactions.type column comment to reflect the
--   domain TransactionType enum values (SUBSCRIPTION_CHARGE | SUBSCRIPTION_REFUND |
--   ALIMTALK_RECHARGE | BILLING_KEY_ISSUE | BILLING_KEY_REVOKE | TAX_INVOICE_ISSUE)
--   instead of the placeholder comment written in V28.
-- =====================================================================

-- B2 — subscriptions: add payment_failure_count
ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS payment_failure_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN subscriptions.payment_failure_count IS
    'Consecutive failed auto-charge attempts. Resets on success or resume. Auto-PAUSE at 3.';

-- B3 — tax_invoices: add failure_reason
ALTER TABLE tax_invoices
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

COMMENT ON COLUMN tax_invoices.failure_reason IS
    'Error message from Popbill / PG when issue_status = ''FAILED''. NULL on PENDING or ISSUED.';

-- N2 — update payment_transactions.type comment to match TransactionType enum
COMMENT ON COLUMN payment_transactions.type IS
    'SUBSCRIPTION_CHARGE | SUBSCRIPTION_REFUND | ALIMTALK_RECHARGE | BILLING_KEY_ISSUE | BILLING_KEY_REVOKE | TAX_INVOICE_ISSUE';
