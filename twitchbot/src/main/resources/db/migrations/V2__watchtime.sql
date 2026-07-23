CREATE TABLE IF NOT EXISTS twitch_watchtime (
    channel_login TEXT NOT NULL,
    user_id       TEXT NOT NULL,
    user_login    TEXT NOT NULL,
    seconds       INTEGER NOT NULL DEFAULT 0,
    updated_at    INTEGER NOT NULL,
    PRIMARY KEY (channel_login, user_id)
);
