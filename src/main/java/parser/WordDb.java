package parser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Code by Shriram Janardhan - Database-backed unique word storage (MySQL)
public final class WordDb {
    private static final String DEFAULT_URL =
        "jdbc:mysql://localhost:3306/sentence_builder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private WordDb() {
    }

    public static Connection openConnection() throws SQLException {
        String url = getSetting("SENTENCE_BUILDER_DB_URL", DEFAULT_URL);
        String user = getSetting("SENTENCE_BUILDER_DB_USER", "root");
        String password = getSetting("SENTENCE_BUILDER_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String getSetting(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return defaultValue;
    }

    // Shriram Janardhan: getOrCreateWordId - atomic upsert for unique word storage
    public static int getOrCreateWordId(String word, Connection conn) throws SQLException {
        if (word == null || word.isBlank()) {
            throw new SQLException("Word must be non-empty");
        }

        // Code by Archisha Sasson
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
        // End of Code by Archisha Sasson
    }

    // Code by Archisha Sasson
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
    // End of Code by Archisha Sasson
}
// End of code by Shriram Janardhan (WordDb, database-backed word storage)
