/* Code by Sammy Pandey ---------------------------------------------

    Purpose: to increment start_count/end_count
 */
package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WordCountsDao {
    private final Connection conn;

    private static final String INC_START = "UPDATE words SET start_count = start_count + 1 WHERE word_id = ?";
    private static final String INC_END   = "UPDATE words SET end_count   = end_count   + 1 WHERE word_id = ?";

    public WordCountsDao(Connection conn) {
        this.conn = conn;
    }

    public void incStart(int wordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INC_START)) {
            ps.setInt(1, wordId);
            ps.executeUpdate();
        }
    }

    public void incEnd(int wordId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INC_END)) {
            ps.setInt(1, wordId);
            ps.executeUpdate();
        }
    }
}
