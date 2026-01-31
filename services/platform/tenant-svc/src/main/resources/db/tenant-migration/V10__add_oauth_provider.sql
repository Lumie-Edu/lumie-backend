-- Add oauth_provider column for OAuth2 login support
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50);

COMMENT ON COLUMN users.oauth_provider IS 'OAuth2 provider name (google, kakao)';
