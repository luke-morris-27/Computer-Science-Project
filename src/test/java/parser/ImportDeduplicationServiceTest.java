package parser;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: ImportDeduplicationServiceTest
 * Created by: Archisha Sasson
 * Description: Verifies duplicate-import preparation decisions for new files, repeated hashes, and invalid paths.
 */
@Tag("unit")
@Tag("task1-person1")
@DisplayName("Import Deduplication Service Tests")
class ImportDeduplicationServiceTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies dedup preparation decisions.");
    }

    @Test
    @DisplayName("Duplicate hash returns DUPLICATE status")
    void duplicateHashReturnsDuplicateStatus() throws Exception {
        Path file = Files.createTempFile("dedup-dup", ".txt");
        Files.writeString(file, "same content");

        ImportDeduplicationService service = new ImportDeduplicationService();
        ImportPreparationResult result = service.prepare(file, hash -> true);

        assertEquals(ImportPreparationStatus.DUPLICATE, result.status());
        assertTrue(result.fileHash() != null && !result.fileHash().isBlank());
    }

    @Test
    @DisplayName("New hash returns READY status")
    void newHashReturnsReadyStatus() throws Exception {
        Path file = Files.createTempFile("dedup-ready", ".txt");
        Files.writeString(file, "new content");

        ImportDeduplicationService service = new ImportDeduplicationService();
        ImportPreparationResult result = service.prepare(file, hash -> false);

        assertEquals(ImportPreparationStatus.READY, result.status());
        assertTrue(result.readyToImport());
    }
}
