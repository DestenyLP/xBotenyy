CREATE TABLE IF NOT EXISTS twitch_quotes
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_login TEXT    NOT NULL,
    quote_number  INTEGER NOT NULL,
    content       TEXT    NOT NULL,
    added_by      TEXT    NOT NULL,
    created_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_twitch_quotes_channel ON twitch_quotes (channel_login, quote_number);
