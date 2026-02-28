package db;

import java.sql.*;

public class WordDao {
    private final Connection conn;

    public WordDao(Connection conn) {
        this.conn = conn;
    }

    public int getOrCreateWordId(String wordText) throws SQLException {
        // 1) try select
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT word_id FROM words WHERE word_text = ?"
        )) {
            ps.setString(1, wordText);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        // 2) insert (unique constraint prevents dupes)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO words (word_text) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            ps.setString(1, wordText);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        // 3) fallback: if two threads inserted at same time, select again
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT word_id FROM words WHERE word_text = ?"
        )) {
            ps.setString(1, wordText);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        throw new SQLException("Could not getOrCreate word_id for: " + wordText);
    }
}