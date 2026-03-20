/*
 * Class: FileDao
 * Created by: Person 2
 * Description: Upserts file-level metadata and returns a stable file_id for downstream stats writes.
 * Example: long fileId = dao.upsertFile(name, path, words, sentences)
 */
package db;

import java.sql.Connection;
import java.sql.SQLException;

public class FileDao {
    private final Connection conn;

    public FileDao(Connection conn) {
        this.conn = conn;
    }

    public long upsertFile(String fileName, String filePath, int wordCount, int sentenceCount) throws SQLException {
        // Guidance:
        // Use INSERT ... ON DUPLICATE KEY UPDATE on (file_name, file_path).
        // Return file_id for the resulting row.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
