CREATE TABLE IF NOT EXISTS twitch_channels (
    channel_login TEXT PRIMARY KEY,
    joined_at     INTEGER NOT NULL,
    last_seen_at  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS twitch_custom_commands (
    channel_login TEXT NOT NULL,
    name          TEXT NOT NULL,
    response      TEXT NOT NULL,
    created_by    TEXT NOT NULL,
    created_at    INTEGER NOT NULL,
    updated_at    INTEGER NOT NULL,
    uses          INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (channel_login, name)
);
