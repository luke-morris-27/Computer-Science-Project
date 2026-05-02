/*
 * Class: FileDao
 * Created by: Shriram Janardhan
 * Description: Upserts file-level metadata and returns a stable file_id for downstream stats writes.
 * Example: long fileId = dao.upsertFile(name, path, words, sentences)
 */
package db;

import java.sql.*;

// Code by Shriram Janardhan
public class FileDao {
    private final Connection conn;

    public FileDao(Connection conn) {
        this.conn = conn;
    }

    public long upsertFile(String fileName, String filePath, int wordCount, int sentenceCount) throws SQLException {

        String sql =
                "INSERT INTO files (file_name, file_path, word_count, sentence_count) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "word_count = VALUES(word_count), " +
                "sentence_count = VALUES(sentence_count)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fileName);
            ps.setString(2, filePath);
            ps.setInt(3, wordCount);
            ps.setInt(4, sentenceCount);

            ps.executeUpdate();

            // Try to get generated key (for new inserts)
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            // If it was an update, fetch the existing file_id
            String lookup = "SELECT file_id FROM files WHERE file_name = ? AND file_path = ?";
            try (PreparedStatement lookupPs = conn.prepareStatement(lookup)) {
                lookupPs.setString(1, fileName);
                lookupPs.setString(2, filePath);

                try (ResultSet rs = lookupPs.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("file_id");
                    }
                }
            }

            throw new SQLException("Unable to retrieve file_id after upsert.");
        }
    }
}
// End of code by Shriram Janardhan (FileDao - upserts file metadata)
