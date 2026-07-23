package de.destenylp.xBotenyy.discordbot.socials;

import de.destenylp.xBotenyy.discordbot.messaging.MessageTemplate;
import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SocialManager extends AbstractSqlManager implements SocialRepository {
    public SocialManager(Database database) {
        super(database);
    }

    @Override
    public List<SocialAccount> getAccounts(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM social_accounts WHERE guild_id = ? ORDER BY name COLLATE NOCASE",
                this::mapAccount, guildId));
    }

    @Override
    public Optional<SocialAccount> getAccount(String guildId, String id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM social_accounts WHERE guild_id = ? AND account_id = ? COLLATE NOCASE",
                this::mapAccount, guildId, id));
    }

    @Override
    public SocialAccount addAccount(String guildId, SocialAccount draft) {
        return database.inTransaction(connection -> {
            String id = generateUniqueShortId(connection, "social_accounts", "account_id", "guild_id", guildId, false);
            draft.assignId(id);
            writeRow(connection, guildId, draft, true);
            return draft;
        });
    }

    @Override
    public void save(String guildId, SocialAccount account) {
        database.runInTransaction(connection -> writeRow(connection, guildId, account, false));
    }

    private void writeRow(java.sql.Connection connection, String guildId, SocialAccount account, boolean insert)
            throws SQLException {
        MessageTemplate yt = account.getYoutubeTemplate();
        MessageTemplate tw = account.getTwitchTemplate();
        if (insert) {
            Jdbc.update(connection, """
                    INSERT INTO social_accounts (guild_id, account_id, name, channel_id, enabled,
                        youtube_channel_id, last_youtube_video_id, yt_embed, yt_title, yt_title_url, yt_author,
                        yt_content, yt_color, yt_image_url, yt_footer, yt_timestamp,
                        twitch_login, last_twitch_stream_id, twitch_currently_live, tw_embed, tw_title,
                        tw_title_url, tw_author, tw_content, tw_color, tw_image_url, tw_footer, tw_timestamp)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    guildId, account.getId(), account.getName(), account.getChannelId(), account.isEnabled(),
                    account.getYoutubeChannelId(), account.getLastYoutubeVideoId(), yt.isEmbed(), yt.getTitle(),
                    yt.getTitleUrl(), yt.getAuthor(), yt.getContent(), yt.getColor(), yt.getImageUrl(),
                    yt.getFooter(), yt.isTimestamp(), account.getTwitchLogin(), account.getLastTwitchStreamId(),
                    account.isTwitchCurrentlyLive(), tw.isEmbed(), tw.getTitle(), tw.getTitleUrl(), tw.getAuthor(),
                    tw.getContent(), tw.getColor(), tw.getImageUrl(), tw.getFooter(), tw.isTimestamp());
        } else {
            Jdbc.update(connection, """
                    UPDATE social_accounts SET name = ?, channel_id = ?, enabled = ?, youtube_channel_id = ?,
                        last_youtube_video_id = ?, yt_embed = ?, yt_title = ?, yt_title_url = ?, yt_author = ?,
                        yt_content = ?, yt_color = ?, yt_image_url = ?, yt_footer = ?, yt_timestamp = ?,
                        twitch_login = ?, last_twitch_stream_id = ?, twitch_currently_live = ?, tw_embed = ?,
                        tw_title = ?, tw_title_url = ?, tw_author = ?, tw_content = ?, tw_color = ?,
                        tw_image_url = ?, tw_footer = ?, tw_timestamp = ?
                    WHERE guild_id = ? AND account_id = ?
                    """,
                    account.getName(), account.getChannelId(), account.isEnabled(), account.getYoutubeChannelId(),
                    account.getLastYoutubeVideoId(), yt.isEmbed(), yt.getTitle(), yt.getTitleUrl(), yt.getAuthor(),
                    yt.getContent(), yt.getColor(), yt.getImageUrl(), yt.getFooter(), yt.isTimestamp(),
                    account.getTwitchLogin(), account.getLastTwitchStreamId(), account.isTwitchCurrentlyLive(),
                    tw.isEmbed(), tw.getTitle(), tw.getTitleUrl(), tw.getAuthor(), tw.getContent(), tw.getColor(),
                    tw.getImageUrl(), tw.getFooter(), tw.isTimestamp(), guildId, account.getId());
        }
    }

    @Override
    public boolean removeAccount(String guildId, String id) {
        int removed = database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM social_accounts WHERE guild_id = ? AND account_id = ? COLLATE NOCASE", guildId, id));
        return removed > 0;
    }

    @Override
    public Map<String, List<SocialAccount>> findAccountsWithYoutube() {
        return database.withConnection(connection -> groupByGuild(Jdbc.query(connection,
                "SELECT * FROM social_accounts WHERE enabled = 1 AND youtube_channel_id IS NOT NULL",
                this::mapGuildAccount)));
    }

    @Override
    public Map<String, List<SocialAccount>> findAccountsWithTwitch() {
        return database.withConnection(connection -> groupByGuild(Jdbc.query(connection,
                "SELECT * FROM social_accounts WHERE enabled = 1 AND twitch_login IS NOT NULL",
                this::mapGuildAccount)));
    }

    private Map<String, List<SocialAccount>> groupByGuild(List<GuildAccount> rows) {
        Map<String, List<SocialAccount>> result = new HashMap<>();
        for (GuildAccount row : rows) {
            result.computeIfAbsent(row.guildId, key -> new ArrayList<>()).add(row.account);
        }
        return result;
    }

    private record GuildAccount(String guildId, SocialAccount account) {
    }

    private GuildAccount mapGuildAccount(ResultSet resultSet) throws SQLException {
        return new GuildAccount(resultSet.getString("guild_id"), mapAccount(resultSet));
    }

    private SocialAccount mapAccount(ResultSet resultSet) throws SQLException {
        SocialAccount account = SocialAccount.builder()
                .name(resultSet.getString("name"))
                .channelId(resultSet.getString("channel_id"))
                .youtubeChannelId(resultSet.getString("youtube_channel_id"))
                .youtubeMessage(resultSet.getString("yt_content"))
                .twitchLogin(resultSet.getString("twitch_login"))
                .twitchMessage(resultSet.getString("tw_content"))
                .build();
        account.assignId(resultSet.getString("account_id"));
        account.setEnabled(Jdbc.getBoolean(resultSet, "enabled"));
        account.setLastYoutubeVideoId(resultSet.getString("last_youtube_video_id"));
        if (resultSet.getString("last_twitch_stream_id") != null
                && Jdbc.getBoolean(resultSet, "twitch_currently_live")) {
            account.markTwitchLive(resultSet.getString("last_twitch_stream_id"));
        }

        MessageTemplate yt = account.getYoutubeTemplate();
        yt.setEmbed(Jdbc.getBoolean(resultSet, "yt_embed"));
        yt.setTitle(resultSet.getString("yt_title"));
        yt.setTitleUrl(resultSet.getString("yt_title_url"));
        yt.setAuthor(resultSet.getString("yt_author"));
        yt.setColor(resultSet.getString("yt_color"));
        yt.setImageUrl(resultSet.getString("yt_image_url"));
        yt.setFooter(resultSet.getString("yt_footer"));
        yt.setTimestamp(Jdbc.getBoolean(resultSet, "yt_timestamp"));

        MessageTemplate tw = account.getTwitchTemplate();
        tw.setEmbed(Jdbc.getBoolean(resultSet, "tw_embed"));
        tw.setTitle(resultSet.getString("tw_title"));
        tw.setTitleUrl(resultSet.getString("tw_title_url"));
        tw.setAuthor(resultSet.getString("tw_author"));
        tw.setColor(resultSet.getString("tw_color"));
        tw.setImageUrl(resultSet.getString("tw_image_url"));
        tw.setFooter(resultSet.getString("tw_footer"));
        tw.setTimestamp(Jdbc.getBoolean(resultSet, "tw_timestamp"));

        return account;
    }
}
