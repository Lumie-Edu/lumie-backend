-- Selected teaching subjects are tenant configuration, stored as JSONB to
-- preserve the multi-select order without introducing a cross-module table.
ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS subjects JSONB NOT NULL DEFAULT '[]'::jsonb;
