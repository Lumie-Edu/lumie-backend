-- Make billing_keys.masked_card_number nullable.
--
-- Reality from Toss Payments: the /v1/billing/authorizations/issue
-- response may legitimately omit `cardNumber` (and `cardCompany`,
-- which is already nullable) — observed reliably on sandbox dummy
-- cards, and Toss documents card-metadata fields as "may be absent"
-- for some payment networks even in production. NOT NULL on the
-- column turns a benign empty-card-metadata response into a
-- constraint violation that aborts BillingKey persistence after the
-- PG call already succeeded, leaking the billing key on Toss's side
-- (the BE never persists, the user can't retry without a fresh
-- authKey, and Toss has issued a key with no FE record).
--
-- The functional invariant is pg_billing_key (NOT NULL stays);
-- masked_card_number is display metadata only.

alter table billing_keys
    alter column masked_card_number drop not null;
