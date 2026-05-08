-- ACADEMY_BOLD template was removed; convert any existing rows to the
-- remaining SOLO_INSTRUCTOR template. The hero section schema is shared,
-- so existing welcomeMessage / contact / image data is preserved.
UPDATE homepage_config SET template_id = 'SOLO_INSTRUCTOR' WHERE template_id = 'ACADEMY_BOLD';
