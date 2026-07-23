package de.destenylp.xBotenyy.common.persistence.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Jdbc {
    private Jdbc() {
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    public static int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    public static <T> List<T> query(Connection connection, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
                return results;
            }
        }
    }

    public static <T> Optional<T> queryOne(Connection connection, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        List<T> results = query(connection, sql, mapper, params);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public static long queryLong(Connection connection, String sql, long fallback, Object... params)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : fallback;
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param instanceof Boolean bool) {
                statement.setInt(i + 1, bool ? 1 : 0);
            } else if (param instanceof Enum<?> enumValue) {
                statement.setString(i + 1, enumValue.name());
            } else {
                statement.setObject(i + 1, param);
            }
        }
    }

    public static String getString(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getString(column);
    }

    public static boolean getBoolean(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getInt(column) != 0;
    }

    public static Integer getNullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
