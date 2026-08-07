CREATE TABLE IF NOT EXISTS moderation_cases
(
    id
    INTEGER
    PRIMARY
    KEY
    AUTOINCREMENT,
    platform
    TEXT
    NOT
    NULL,
    scope_id
    TEXT
    NOT
    NULL,
    target_id
    TEXT
    NOT
    NULL,
    target_name
    TEXT,
    moderator_id
    TEXT,
    moderator_name
    TEXT,
    action
    TEXT
    NOT
    NULL,
    reason
    TEXT,
    duration_seconds
    INTEGER
    NOT
    NULL
    DEFAULT
    0,
    created_at
    INTEGER
    NOT
    NULL,
    active
    INTEGER
    NOT
    NULL
    DEFAULT
    1,
    synced
    INTEGER
    NOT
    NULL
    DEFAULT
    0
);

CREATE INDEX IF NOT EXISTS idx_moderation_cases_target ON moderation_cases (platform, scope_id, target_id, created_at);
CREATE INDEX IF NOT EXISTS idx_moderation_cases_active ON moderation_cases (platform, scope_id, target_id, action, active);

CREATE TABLE IF NOT EXISTS account_links
(
    discord_user_id
    TEXT
    PRIMARY
    KEY,
    twitch_user_id
    TEXT
    NOT
    NULL,
    twitch_login
    TEXT,
    linked_at
    INTEGER
    NOT
    NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_account_links_twitch ON account_links (twitch_user_id);

CREATE TABLE IF NOT EXISTS pending_link_verifications
(
    code
    TEXT
    PRIMARY
    KEY,
    discord_user_id
    TEXT,
    discord_username
    TEXT,
    twitch_user_id
    TEXT,
    twitch_login
    TEXT,
    initiated_from
    TEXT
    NOT
    NULL,
    expires_at
    INTEGER
    NOT
    NULL
);
