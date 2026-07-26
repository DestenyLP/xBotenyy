package de.destenylp.xBotenyy.twitchbot.persistence;

import de.destenylp.xBotenyy.common.persistence.sql.AbstractSqlManager;
import de.destenylp.xBotenyy.common.persistence.sql.Database;
import de.destenylp.xBotenyy.common.persistence.sql.Jdbc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class TwitchQuoteRepository extends AbstractSqlManager {
    public TwitchQuoteRepository(Database database) {
        super(database);
    }

    private static QuoteRecord mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new QuoteRecord(
                resultSet.getInt("quote_number"),
                resultSet.getString("content"),
                resultSet.getString("added_by"),
                resultSet.getLong("created_at"));
    }

    public QuoteRecord add(String channelLogin, String content, String addedBy) {
        long now = Instant.now().toEpochMilli();
        int nextNumber = nextQuoteNumber(channelLogin);
        database.useConnection(connection -> Jdbc.update(connection,
                "INSERT INTO twitch_quotes (channel_login, quote_number, content, added_by, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                channelLogin, nextNumber, content, addedBy, now));
        return new QuoteRecord(nextNumber, content, addedBy, now);
    }

    public Optional<QuoteRecord> get(String channelLogin, int quoteNumber) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT quote_number, content, added_by, created_at FROM twitch_quotes "
                        + "WHERE channel_login = ? AND quote_number = ?",
                TwitchQuoteRepository::mapRow, channelLogin, quoteNumber));
    }

    public Optional<QuoteRecord> random(String channelLogin) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                "SELECT quote_number, content, added_by, created_at FROM twitch_quotes "
                        + "WHERE channel_login = ? ORDER BY RANDOM() LIMIT 1",
                TwitchQuoteRepository::mapRow, channelLogin));
    }

    public boolean remove(String channelLogin, int quoteNumber) {
        return database.withConnection(connection -> Jdbc.update(connection,
                "DELETE FROM twitch_quotes WHERE channel_login = ? AND quote_number = ?",
                channelLogin, quoteNumber)) > 0;
    }

    public int count(String channelLogin) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                        "SELECT COUNT(*) AS total FROM twitch_quotes WHERE channel_login = ?",
                        resultSet -> resultSet.getInt("total"), channelLogin))
                .orElse(0);
    }

    public List<QuoteRecord> list(String channelLogin, int limit) {
        return database.withConnection(connection -> Jdbc.query(connection,
                "SELECT quote_number, content, added_by, created_at FROM twitch_quotes "
                        + "WHERE channel_login = ? ORDER BY quote_number ASC LIMIT ?",
                TwitchQuoteRepository::mapRow, channelLogin, limit));
    }

    private int nextQuoteNumber(String channelLogin) {
        return database.withConnection(connection -> Jdbc.queryOne(connection,
                        "SELECT COALESCE(MAX(quote_number), 0) + 1 AS next_number FROM twitch_quotes "
                                + "WHERE channel_login = ?",
                        resultSet -> resultSet.getInt("next_number"), channelLogin))
                .orElse(1);
    }

    public record QuoteRecord(int quoteNumber, String content, String addedBy, long createdAt) {
    }
}
