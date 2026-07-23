CREATE TABLE IF NOT EXISTS twitch_broadcasts (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_login    TEXT NOT NULL,
    message          TEXT NOT NULL,
    interval_seconds INTEGER NOT NULL,
    min_messages     INTEGER NOT NULL DEFAULT 0,
    enabled          INTEGER NOT NULL DEFAULT 1,
    created_by       TEXT NOT NULL,
    created_at       INTEGER NOT NULL,
    updated_at       INTEGER NOT NULL,
    last_sent_at     INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_twitch_broadcasts_channel ON twitch_broadcasts (channel_login);
