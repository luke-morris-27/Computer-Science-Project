/*
 * Class: WordFileStatsDao
 * Created by: Person 2
 * Description: Performs batch upserts of per-file word statistics into word_file_stats.
 * Example: dao.upsertStats(fileId, statsRows)
 */
package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

public class WordFileStatsDao {
    private final Connection conn;

    public WordFileStatsDao(Connection conn) {
        this.conn = conn;
    }

    public void upsertStats(long fileId, Collection<WordFileStatInput> stats) throws SQLException {
        // Guidance:
        // For each row, insert or update count_in_file/start_in_file/end_in_file.
        // Use batch execution for efficiency.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
