/*
 * Class: ImportDao
 * Created by: Person 1
 * Description: Reads and writes import metadata rows, especially for file_hash duplicate checks.
 * Example: boolean seen = new ImportDao(conn).existsByHash(hash)
 */
package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class ImportDao {
    private final Connection conn;

    public ImportDao(Connection conn) {
        this.conn = conn;
    }

    public boolean existsByHash(String fileHash) throws SQLException {
        // Guidance:
        // Query imports by file_hash and return whether at least one row exists.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public long insertImport(String filename, int wordCount, Instant importedAt, String fileHash) throws SQLException {
        // Guidance:
        // Insert into imports and return generated import_id.
        // Use importedAt when provided; otherwise use current timestamp.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
