-- Sidebar visibility is no longer configurable per tenant. This permanently
-- removes the previously stored per-tenant hide selections.
ALTER TABLE tenants DROP COLUMN IF EXISTS hidden_sidebar_item_ids;
