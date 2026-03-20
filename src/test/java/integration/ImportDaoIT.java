package integration;

import java.sql.Connection;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import db.ImportDao;
import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Tests for Task 1, Person 1.
 */
@Tag("integration")
@Tag("task1-person1")
@DisplayName("Import DAO Integration Tests")
class ImportDaoIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Insert import row and find it by hash")
    void insertAndLookupByHash() throws Exception {
        try (Connection conn = openConnection()) {
            ImportDao dao = new ImportDao(conn);
            String fileHash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

            assertFalse(dao.existsByHash(fileHash));

            long importId = dao.insertImport("sample.txt", 42, Instant.parse("2026-03-10T00:00:00Z"), fileHash);
            assertTrue(importId > 0);
            assertTrue(dao.existsByHash(fileHash));

            assertEquals(1, queryForInt("SELECT COUNT(*) FROM imports WHERE file_hash = ?", fileHash));
        }
    }
}
