package parser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC connections to MySQL/MariaDB. Configure URL, user, and password via {@link DatabaseConfig}
 * (system properties, environment variables, optional root {@code .env}, or classpath {@code database.properties}).
 */
public final class WordDb {

    private WordDb() {
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            DatabaseConfig.resolveJdbcUrl(),
            DatabaseConfig.resolveUsername(),
            DatabaseConfig.resolvePassword()
        );
    }

    public static int getOrCreateWordId(String word, Connection conn) throws SQLException {
        if (word == null || word.isBlank()) {
            throw new SQLException("Word must be non-empty");
        }

        Integer existingId = findWordId(word, conn);
        if (existingId != null) {
            return existingId;
        }

        try (PreparedStatement insert = conn.prepareStatement(
            "INSERT INTO words (word_text) VALUES (?)",
            Statement.RETURN_GENERATED_KEYS
        )) {
            insert.setString(1, word);
            insert.executeUpdate();

            try (ResultSet generatedKeys = insert.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            throw new SQLException("Failed to retrieve generated word_id for: " + word);
        } catch (SQLException e) {
            Integer concurrentId = findWordId(word, conn);
            if (concurrentId != null) {
                return concurrentId;
            }
            throw e;
        }
    }

    private static Integer findWordId(String word, Connection conn) throws SQLException {
        try (PreparedStatement lookup = conn.prepareStatement(
            "SELECT word_id FROM words WHERE word_text = ?"
        )) {
            lookup.setString(1, word);
            try (ResultSet rs = lookup.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("word_id");
                }
                return null;
            }
        }
    }
}
