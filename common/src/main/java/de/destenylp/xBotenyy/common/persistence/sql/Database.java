package de.destenylp.xBotenyy.common.persistence.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

public final class Database implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final Connection connection;
    private final Path databaseFile;

    private Database(Connection connection, Path databaseFile) {
        this.connection = connection;
        this.databaseFile = databaseFile;
    }

    public static Database open(Path databaseFile) {
        try {
            if (databaseFile.getParent() != null) {
                Files.createDirectories(databaseFile.getParent());
            }

            SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.enforceForeignKeys(true);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            config.setBusyTimeout(10_000);

            Connection connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databaseFile.toAbsolutePath(), config.toProperties());

            Database database = new Database(connection, databaseFile);
            SchemaMigrator.migrate(database);
            LOGGER.info("SQLite Datenbank geoeffnet: {}", databaseFile.toAbsolutePath());
            return database;
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Konnte SQLite Datenbank " + databaseFile + " nicht oeffnen", e);
        }
    }

    public Path getDatabaseFile() {
        return databaseFile;
    }

    Connection rawConnection() {
        return connection;
    }

    public <T> T withConnection(SqlFunction<Connection, T> work) {
        lock.lock();
        try {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseException("SQL Fehler", e);
        } finally {
            lock.unlock();
        }
    }

    public void useConnection(SqlConsumer<Connection> work) {
        withConnection(connection -> {
            work.accept(connection);
            return null;
        });
    }

    public <T> T inTransaction(SqlFunction<Connection, T> work) {
        lock.lock();
        try {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                connection.rollback();
                throw new DatabaseException("Transaktion fehlgeschlagen, Rollback ausgefuehrt", e);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("SQL Fehler beim Transaktionsmanagement", e);
        } finally {
            lock.unlock();
        }
    }

    public void runInTransaction(SqlConsumer<Connection> work) {
        inTransaction(connection -> {
            work.accept(connection);
            return null;
        });
    }

    public void backupInto(Path targetFile) {
        withConnection(connection -> {
            try {
                if (targetFile.getParent() != null) {
                    Files.createDirectories(targetFile.getParent());
                }
                Files.deleteIfExists(targetFile);
            } catch (IOException e) {
                throw new DatabaseException("Konnte Backup-Zielverzeichnis nicht vorbereiten", e);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("VACUUM INTO '" + targetFile.toAbsolutePath().toString().replace("'", "''") + "'");
            }
            return null;
        });
    }

    public void vacuum() {
        withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("VACUUM");
            }
            return null;
        });
    }

    @Override
    public void close() {
        lock.lock();
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.warn("Fehler beim Schliessen der Datenbank: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T input) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T input) throws SQLException;
    }

    public static final class DatabaseException extends RuntimeException {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
