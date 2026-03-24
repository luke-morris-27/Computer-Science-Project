package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import parser.WordDb;

public class ImportDaoIT {

    @BeforeEach
    void cleanImports() throws SQLException {
        try (Connection conn = WordDb.openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM imports");
        }
    }

    @Test
    void insertAndExistsByHashWorks() throws SQLException {
        try (Connection conn = WordDb.openConnection()) {
            ImportDao dao = new ImportDao(conn);

            String hash = "test-hash-123";
            assertFalse(dao.existsByHash(hash));

            long id = dao.insertImport("test.txt", 10, Instant.now(), hash);
            assertTrue(id > 0);

            assertTrue(dao.existsByHash(hash));
            assertFalse(dao.existsByHash("other-hash"));
        }
    }
}
