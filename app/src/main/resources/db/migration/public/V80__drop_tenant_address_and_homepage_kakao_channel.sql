-- Contract migration: application code no longer reads tenant physical address
-- or the homepage hero KakaoTalk channel URL. Existing values are intentionally
-- discarded.
ALTER TABLE tenants DROP COLUMN IF EXISTS address;

UPDATE homepage_config
SET sections = sections #- '{hero,kakaoChannelUrl}'
WHERE sections #> '{hero,kakaoChannelUrl}' IS NOT NULL;
