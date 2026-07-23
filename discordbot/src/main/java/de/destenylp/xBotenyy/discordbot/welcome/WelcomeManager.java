package de.destenylp.xBotenyy.discordbot.welcome;

import de.destenylp.xBotenyy.discordbot.messaging.MessageTemplate;
import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class WelcomeManager extends AbstractSqlManager implements WelcomeRepository {
    public WelcomeManager(Database database) {
        super(database);
    }

    @Override
    public Optional<WelcomeSettings> getSettings(String guildId) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM welcome_guild_settings WHERE guild_id = ?",
                resultSet -> mapSettings(guildId, resultSet), guildId));
    }

    private WelcomeSettings mapSettings(String guildId, ResultSet resultSet) throws SQLException {
        WelcomeSettings settings = new WelcomeSettings(() -> getVariants(guildId));
        settings.setChannelId(Jdbc.getString(resultSet, "channel_id"));
        settings.setEnabled(Jdbc.getBoolean(resultSet, "enabled"));
        settings.setDmEnabled(Jdbc.getBoolean(resultSet, "dm_enabled"));
        return settings;
    }

    @Override
    public void updateChannel(String guildId, String channelId) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "welcome_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE welcome_guild_settings SET channel_id = ? WHERE guild_id = ?",
                    channelId, guildId);
        });
    }

    @Override
    public void updateEnabled(String guildId, boolean enabled) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "welcome_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE welcome_guild_settings SET enabled = ? WHERE guild_id = ?",
                    enabled, guildId);
        });
    }

    @Override
    public void updateDm(String guildId, boolean dmEnabled) {
        database.runInTransaction(connection -> {
            ensureSettingsRow(connection, "welcome_guild_settings", guildId);
            Jdbc.update(connection, "UPDATE welcome_guild_settings SET dm_enabled = ? WHERE guild_id = ?",
                    dmEnabled, guildId);
        });
    }

    @Override
    public WelcomeVariant addVariant(String guildId, WelcomeVariant draft) {
        return database.inTransaction(connection -> {
            ensureSettingsRow(connection, "welcome_guild_settings", guildId);
            String id = generateUniqueShortId(connection, "welcome_variants", "variant_id", "guild_id", guildId, false);
            draft.assignId(id);
            insertVariant(connection, guildId, draft);
            return draft;
        });
    }

    private void insertVariant(Connection connection, String guildId, WelcomeVariant variant) throws SQLException {
        MessageTemplate template = variant.getTemplate();
        Jdbc.update(connection, """
                INSERT INTO welcome_variants (guild_id, variant_id, ping, embed, title, content, color, image_url,
                    footer)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, guildId, variant.getId(), variant.isPing(), template.isEmbed(), template.getTitle(),
                template.getContent(), template.getColor(), template.getImageUrl(), template.getFooter());
    }

    @Override
    public void save(String guildId, WelcomeVariant variant) {
        MessageTemplate template = variant.getTemplate();
        database.runInTransaction(connection -> Jdbc.update(connection, """
                UPDATE welcome_variants SET ping = ?, embed = ?, title = ?, content = ?, color = ?, image_url = ?,
                    footer = ?
                WHERE guild_id = ? AND variant_id = ?
                """, variant.isPing(), template.isEmbed(), template.getTitle(), template.getContent(),
                template.getColor(), template.getImageUrl(), template.getFooter(), guildId, variant.getId()));
    }

    @Override
    public Optional<WelcomeVariant> getVariant(String guildId, String id) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT * FROM welcome_variants WHERE guild_id = ? AND variant_id = ? COLLATE NOCASE",
                this::mapVariant, guildId, id));
    }

    @Override
    public boolean removeVariant(String guildId, String id) {
        int removed = database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM welcome_variants WHERE guild_id = ? AND variant_id = ? COLLATE NOCASE", guildId, id));
        return removed > 0;
    }

    @Override
    public List<WelcomeVariant> getVariants(String guildId) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT * FROM welcome_variants WHERE guild_id = ?", this::mapVariant, guildId));
    }

    @Override
    public Optional<WelcomeVariant> getRandomVariant(String guildId) {
        List<WelcomeVariant> variants = getVariants(guildId);
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(variants.get(ThreadLocalRandom.current().nextInt(variants.size())));
    }

    private WelcomeVariant mapVariant(ResultSet resultSet) throws SQLException {
        WelcomeVariant variant = WelcomeVariant.builder()
                .ping(Jdbc.getBoolean(resultSet, "ping"))
                .embed(Jdbc.getBoolean(resultSet, "embed"))
                .title(resultSet.getString("title"))
                .content(resultSet.getString("content"))
                .color(resultSet.getString("color"))
                .imageUrl(resultSet.getString("image_url"))
                .footer(resultSet.getString("footer"))
                .build();
        variant.assignId(resultSet.getString("variant_id"));
        return variant;
    }
}
