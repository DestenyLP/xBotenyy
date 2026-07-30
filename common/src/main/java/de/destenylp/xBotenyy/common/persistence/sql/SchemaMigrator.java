package de.destenylp.xBotenyy.common.persistence.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SchemaMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaMigrator.class);
    private static final Pattern MIGRATION_FILENAME = Pattern.compile("V(\\d+)__(.+)\\.sql");

    private SchemaMigrator() {
    }

    public static void migrate(Database database, String migrationsResourcePath) {
        List<Migration> migrations = discoverMigrations(migrationsResourcePath);
        if (migrations.isEmpty()) {
            throw new IllegalStateException("Keine Migrationsdateien unter " + migrationsResourcePath
                    + " gefunden, Datenbankschema kann nicht initialisiert werden.");
        }

        database.runInTransaction(connection -> ensureMigrationsTable(connection));

        for (Migration migration : migrations) {
            boolean alreadyApplied = database.withConnection(connection -> isApplied(connection, migration.version()));
            if (alreadyApplied) {
                continue;
            }
            applyMigration(database, migration);
        }
    }

    private static void applyMigration(Database database, Migration migration) {
        LOGGER.info("Wende Migration V{} ({}) an ...", migration.version(), migration.description());
        database.runInTransaction(connection -> {
            for (String statement : splitStatements(migration.sql())) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(statement);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO schema_migrations (version, description, applied_at) VALUES (?, ?, ?)")) {
                insert.setInt(1, migration.version());
                insert.setString(2, migration.description());
                insert.setLong(3, Instant.now().toEpochMilli());
                insert.executeUpdate();
            }
        });
        LOGGER.info("Migration V{} erfolgreich angewendet.", migration.version());
    }

    private static void ensureMigrationsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version     INTEGER PRIMARY KEY,
                        description TEXT NOT NULL,
                        applied_at  INTEGER NOT NULL
                    )
                    """);
        }
    }

    private static boolean isApplied(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT 1 FROM schema_migrations WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(current.toString().strip());
                current.setLength(0);
            }
        }
        if (!current.isEmpty() && !current.toString().isBlank()) {
            statements.add(current.toString().strip());
        }
        return statements.stream()
                .map(statement -> statement.endsWith(";") ? statement.substring(0, statement.length() - 1) : statement)
                .filter(statement -> !statement.isBlank())
                .toList();
    }

    private static List<Migration> discoverMigrations(String migrationsResourcePath) {
        try {
            URL resource = SchemaMigrator.class.getClassLoader().getResource(migrationsResourcePath);
            if (resource == null) {
                return List.of();
            }

            List<String> fileNames = new ArrayList<>();
            if ("jar".equals(resource.getProtocol())) {
                fileNames.addAll(listFromJar(resource, migrationsResourcePath));
            } else {
                fileNames.addAll(listFromFilesystem(resource));
            }

            List<Migration> migrations = new ArrayList<>();
            for (String fileName : fileNames) {
                Matcher matcher = MIGRATION_FILENAME.matcher(fileName);
                if (!matcher.matches()) {
                    continue;
                }
                int version = Integer.parseInt(matcher.group(1));
                String description = matcher.group(2).replace('_', ' ');
                String sql = readResource(migrationsResourcePath + "/" + fileName);
                migrations.add(new Migration(version, description, sql));
            }
            migrations.sort(Comparator.comparingInt(Migration::version));
            return migrations;
        } catch (IOException | URISyntaxException e) {
            throw new UncheckedIOException("Konnte Migrationen nicht auflisten", e instanceof IOException io ? io
                    : new IOException(e));
        }
    }

    private static List<String> listFromFilesystem(URL resource) throws URISyntaxException, IOException {
        Path directory = Path.of(resource.toURI());
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString()).toList();
        }
    }

    private static List<String> listFromJar(URL resource, String migrationsResourcePath) throws IOException, URISyntaxException {
        String[] parts = resource.toURI().toString().split("!");
        java.net.URI jarUri = java.net.URI.create(parts[0]);

        FileSystem fileSystem;
        boolean createdByUs;
        try {
            fileSystem = FileSystems.newFileSystem(jarUri, Map.of());
            createdByUs = true;
        } catch (FileSystemAlreadyExistsException e) {
            fileSystem = FileSystems.getFileSystem(jarUri);
            createdByUs = false;
        }

        try {
            Path directory = fileSystem.getPath(migrationsResourcePath);
            try (Stream<Path> files = Files.list(directory)) {
                return files.map(path -> path.getFileName().toString()).toList();
            }
        } finally {
            if (createdByUs) {
                fileSystem.close();
            }
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = SchemaMigrator.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Ressource nicht gefunden: " + path);
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            return builder.toString();
        }
    }

    private record Migration(int version, String description, String sql) {
    }
}