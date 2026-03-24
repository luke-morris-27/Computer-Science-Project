/*
 * Class: WordFileStatsDao
 * Created by: Shriram Janardhan
 * Description: Performs batch upserts of per-file word statistics into word_file_stats.
 * Example: dao.upsertStats(fileId, statsRows)
 */
package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

// Code by Shriram Janardhan
public class WordFileStatsDao {
    private final Connection conn;

    public WordFileStatsDao(Connection conn) {
        this.conn = conn;
    }

    public void upsertStats(long fileId, Collection<WordFileStatInput> stats) throws SQLException {

        String sql =
                "INSERT INTO word_file_stats (word_id, file_id, count_in_file, start_in_file, end_in_file) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "count_in_file = VALUES(count_in_file), " +
                "start_in_file = VALUES(start_in_file), " +
                "end_in_file = VALUES(end_in_file)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (WordFileStatInput stat : stats) {

                ps.setInt(1, stat.wordId());
                ps.setLong(2, fileId);
                ps.setInt(3, stat.countInFile());
                ps.setInt(4, stat.startInFile());
                ps.setInt(5, stat.endInFile());

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }
}
// End of code by Shriram Janardhan (WordFileStatsDao - batch upserts per-file word stats)
