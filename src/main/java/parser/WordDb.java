package parser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

        try (PreparedStatement upsert = conn.prepareStatement(
            "INSERT INTO words (word_text) VALUES (?) " +
                "ON DUPLICATE KEY UPDATE word_id = LAST_INSERT_ID(word_id)"
        )) {
            upsert.setString(1, word);
            upsert.executeUpdate();
        }

        try (PreparedStatement lastId = conn.prepareStatement("SELECT LAST_INSERT_ID()");
             ResultSet rs = lastId.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("Failed to retrieve LAST_INSERT_ID()");
            }
            return rs.getInt(1);
        }
    }
}
// End of code by Shriram Janardhan (WordDb, database-backed word storage)
