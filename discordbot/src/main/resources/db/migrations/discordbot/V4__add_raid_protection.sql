PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS raid_protection_settings
(
    guild_id TEXT PRIMARY KEY,
    enabled INTEGER NOT NULL DEFAULT 0,
    alert_channel_id TEXT
);
