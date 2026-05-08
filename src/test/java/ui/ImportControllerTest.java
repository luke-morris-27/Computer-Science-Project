package ui;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import parser.ImportDeduplicationService;
import parser.ImportPreparationResult;
import parser.ImportPreparationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: ImportControllerTest
 * Created by: Archisha Sasson
 * Description: Verifies import controller validation for valid files and missing-file error cases.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Import Controller Tests")
class ImportControllerTest {
    private final ImportController controller =
        new ImportController(new ImportDeduplicationService(), hash -> false);

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies import screen validation states.");
    }

    @Test
    @DisplayName("Valid file path returns ready state")
    void validFilePathPassesValidation() throws Exception {
        Path file = Files.createTempFile("ui-import", ".txt");
        Files.writeString(file, "hello");

        ImportViewState state = controller.validatePath(file.toString());

        assertTrue(state.valid());
        assertEquals("File is ready for import.", state.message());
    }

    @Test
    @DisplayName("Missing file path returns validation error")
    void missingFilePathFailsValidation() {
        ImportViewState state = controller.validatePath("/does/not/exist.txt");
        assertEquals(false, state.valid());
    }

    // Code by Shriram
    @Test
    @DisplayName("First import of a file is marked ready and not duplicate")
    void firstImportOfFileIsNotDuplicate() throws Exception {
        ImportController freshController =
            new ImportController(new ImportDeduplicationService(), hash -> false);
        Path file = Files.createTempFile("dedup-first", ".txt");
        Files.writeString(file, "hello world");

        ImportPreparationResult prep = freshController.prepareImport(file);

        assertEquals(ImportPreparationStatus.READY, prep.status());
        assertTrue(prep.readyToImport());
    }

    // Deleted due to code cleanup: duplicate/useless code
    // End of Code by Shriram
}
