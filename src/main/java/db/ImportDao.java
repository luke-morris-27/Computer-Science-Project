/*
 * Class: ImportDao
 * Created by: Person 1
 * Description: Reads and writes import metadata rows, especially for file_hash duplicate checks.
 * Example: boolean seen = new ImportDao(conn).existsByHash(hash)
 */
package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;


public class ImportDao {
    private final Connection conn;

    public ImportDao(Connection conn) {
        this.conn = conn;
    }

    public boolean existsByHash(String fileHash) throws SQLException {
        String sql = "SELECT 1 FROM imports WHERE file_hash = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertImport(String filename, int wordCount, Instant importedAt, String fileHash)
            throws SQLException {

        String sql = "INSERT INTO imports (filename, word_count, imported_at, file_hash) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, filename);
            ps.setInt(2, wordCount);
            Timestamp ts = importedAt != null
                    ? Timestamp.from(importedAt)
                    : Timestamp.from(Instant.now());
            ps.setTimestamp(3, ts);
            ps.setString(4, fileHash);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new SQLException("Failed to retrieve generated import_id");
        }
    }
}
