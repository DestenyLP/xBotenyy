CREATE TABLE IF NOT EXISTS twitch_event_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_login TEXT NOT NULL,
    event_type    TEXT NOT NULL,
    actor_id      TEXT,
    detail        TEXT NOT NULL,
    created_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_twitch_event_log_channel ON twitch_event_log (channel_login, created_at);
