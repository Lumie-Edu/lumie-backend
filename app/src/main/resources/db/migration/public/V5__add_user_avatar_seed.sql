-- Add avatar_seed column to users for the curated DiceBear notionists avatar
-- picker on the profile settings page. Stores a short string (one of
-- AVATAR_SEEDS in src/shared/lib/avatar.ts) that the frontend resolves to a
-- DiceBear CDN URL on the client.

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_seed VARCHAR(50);
