ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS hidden_sidebar_item_ids jsonb NOT NULL DEFAULT '[]'::jsonb;
