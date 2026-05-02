package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NextWordDao {
    private final Connection conn;

    // Code by Archisha Sasson
    private static final String INSERT =
            "INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end) " +
                    "VALUES (?, ?, 1, ?, ?)";

    private static final String UPDATE =
            "UPDATE next_word " +
                    "SET transition_count = transition_count + 1, " +
                    "    follows_sentence_start = CASE WHEN follows_sentence_start OR ? THEN TRUE ELSE FALSE END " +
                    "WHERE from_word_id = ? AND to_word_id = ?";
    // End of Code by Archisha Sasson

    private static final String MARK_PRECEDES_END =
            "UPDATE next_word SET precedes_sentence_end = TRUE WHERE from_word_id = ? AND to_word_id = ?"; // Sammy Pandey 2/27

    private static final String UPSERT_INCREMENT_BY =
        "INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end) "
            + "VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "transition_count = transition_count + VALUES(transition_count), "
            + "follows_sentence_start = (follows_sentence_start OR VALUES(follows_sentence_start)), "
            + "precedes_sentence_end = (precedes_sentence_end OR VALUES(precedes_sentence_end))";

    public NextWordDao(Connection conn) {
        this.conn = conn;
    }

    public void increment(int fromId, int toId, boolean followsStart) throws SQLException {
        // Code by Archisha Sasson
        if (updateExistingRow(fromId, toId, followsStart) > 0) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setBoolean(3, followsStart);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (updateExistingRow(fromId, toId, followsStart) == 0) {
                throw e;
            }
        }
        // End of Code by Archisha Sasson
    }

    public void markPrecedesEnd(int fromId, int toId) throws SQLException { // Sammy Pandey 2/27
        try (PreparedStatement ps = conn.prepareStatement(MARK_PRECEDES_END)) { // Sammy Pandey 2/27
            ps.setInt(1, fromId); // Sammy Pandey 2/27
            ps.setInt(2, toId);   // Sammy Pandey 2/27
            ps.executeUpdate();   // Sammy Pandey 2/27
        }
    }

    public void incrementBy(int fromId, int toId, int amount, boolean followsStart, boolean precedesEnd) throws SQLException {
        if (amount <= 0) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_INCREMENT_BY)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setInt(3, amount);
            ps.setBoolean(4, followsStart);
            ps.setBoolean(5, precedesEnd);
            ps.executeUpdate();
        }
    }

    // Code by Archisha Sasson
    private int updateExistingRow(int fromId, int toId, boolean followsStart) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setBoolean(1, followsStart);
            ps.setInt(2, fromId);
            ps.setInt(3, toId);
            return ps.executeUpdate();
        }
    }
    // End of Code by Archisha Sasson
}
