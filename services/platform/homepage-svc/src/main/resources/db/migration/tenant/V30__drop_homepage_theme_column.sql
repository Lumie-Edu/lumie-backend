-- Theme/color customization was removed from the homepage feature.
-- All branding now uses a fixed monochrome design baked into the templates.
ALTER TABLE homepage_config DROP COLUMN IF EXISTS theme;
