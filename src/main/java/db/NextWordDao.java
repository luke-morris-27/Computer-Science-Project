package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NextWordDao {
    private final Connection conn;

    private static final String UPSERT =
            "INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end) " +
                    "VALUES (?, ?, 1, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "  transition_count = transition_count + 1, " +
                    "  follows_sentence_start = follows_sentence_start OR VALUES(follows_sentence_start), " +
                    "  precedes_sentence_end  = precedes_sentence_end  OR VALUES(precedes_sentence_end)";

    private static final String MARK_PRECEDES_END =
            "UPDATE next_word SET precedes_sentence_end = TRUE WHERE from_word_id = ? AND to_word_id = ?"; // Sammy Pandey 2/27

    public NextWordDao(Connection conn) {
        this.conn = conn;
    }

    public void increment(int fromId, int toId, boolean followsStart, boolean precedesEnd) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPSERT)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setBoolean(3, followsStart);
            ps.setBoolean(4, precedesEnd);
            ps.executeUpdate();
        }
    }

    public void markPrecedesEnd(int fromId, int toId) throws SQLException { // Sammy Pandey 2/27
        try (PreparedStatement ps = conn.prepareStatement(MARK_PRECEDES_END)) { // Sammy Pandey 2/27
            ps.setInt(1, fromId); // Sammy Pandey 2/27
            ps.setInt(2, toId);   // Sammy Pandey 2/27
            ps.executeUpdate();   // Sammy Pandey 2/27
        }
    }
}