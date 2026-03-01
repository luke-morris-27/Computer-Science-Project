package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WordQueryDao {

    private final Connection conn;

    public WordQueryDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * Returns the word_id for a given word text.
     * Returns null if not found.
     */
    public Integer getWordId(String wordText) throws SQLException {
        String sql = "SELECT word_id FROM words WHERE word_text = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wordText);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("word_id");
                }
                return null;
            }
        }
    }

    /**
     * Returns all next words ordered by:
     * 1) highest transition_count first
     * 2) lowest to_word_id as deterministic tie breaker
     */
    public List<NextWord> getNextWords(int wordId) throws SQLException {

        String sql = """
            SELECT nw.to_word_id,
                   w.word_text,
                   nw.transition_count
            FROM next_word nw
            JOIN words w ON nw.to_word_id = w.word_id
            WHERE nw.from_word_id = ?
            ORDER BY nw.transition_count DESC,
                     nw.to_word_id ASC
        """;

        List<NextWord> results = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wordId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new NextWord(
                            rs.getInt("to_word_id"),
                            rs.getString("word_text"),
                            rs.getInt("transition_count")
                    ));
                }
            }
        }

        return results;
    }

    /**
     * Returns words that can start sentences.
     * Ordered deterministically by:
     * 1) highest start_count
     * 2) lowest word_id
     */
    public List<NextWord> getStartWords() throws SQLException {

        String sql = """
            SELECT word_id,
                   word_text,
                   start_count
            FROM words
            WHERE start_count > 0
            ORDER BY start_count DESC,
                     word_id ASC
        """;

        List<NextWord> results = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(new NextWord(
                        rs.getInt("word_id"),
                        rs.getString("word_text"),
                        rs.getInt("start_count")
                ));
            }
        }

        return results;
    }
}
