ALTER TABLE social_accounts
    ADD COLUMN tiktok_username TEXT;
ALTER TABLE social_accounts
    ADD COLUMN last_tiktok_video_id TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_embed INTEGER NOT NULL DEFAULT 1;
ALTER TABLE social_accounts
    ADD COLUMN tt_title TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_title_url TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_author TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_content TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_color TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_image_url TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_footer TEXT;
ALTER TABLE social_accounts
    ADD COLUMN tt_timestamp INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_social_tiktok ON social_accounts (enabled, tiktok_username);
