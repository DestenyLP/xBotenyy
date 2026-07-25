ALTER TABLE twitch_custom_commands
    ADD COLUMN cooldown_seconds INTEGER NOT NULL DEFAULT 5;
