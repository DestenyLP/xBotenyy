ALTER TABLE social_accounts
    ADD COLUMN yt_ping TEXT DEFAULT '@everyone';
ALTER TABLE social_accounts
    ADD COLUMN tw_ping TEXT DEFAULT '@everyone';
ALTER TABLE social_accounts
    ADD COLUMN tt_ping TEXT DEFAULT '@everyone';
