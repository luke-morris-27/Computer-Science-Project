package ui;

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
 * Class: ImportControllerTest
 * Created by: Archisha Sasson
 * Description: Verifies import controller validation for valid files and missing-file error cases.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Import Controller Tests")
class ImportControllerTest {
    private final ImportController controller = new ImportController();

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
        ImportController freshController = new ImportController();
        Path file = Files.createTempFile("dedup-first", ".txt");
        Files.writeString(file, "hello world");

        ImportViewState state = freshController.checkForDuplicate(file);

        assertTrue(state.valid());
        assertEquals(false, state.duplicate());
    }

    @Test
    @DisplayName("Second import of the same file is flagged as duplicate")
    void secondImportOfFileIsDuplicate() throws Exception {
        ImportController freshController = new ImportController();
        Path file = Files.createTempFile("dedup-second", ".txt");
        Files.writeString(file, "hello world");

        freshController.checkForDuplicate(file);
        ImportViewState state = freshController.checkForDuplicate(file);

        assertEquals(false, state.valid());
        assertTrue(state.duplicate());
        assertEquals("This file has already been imported.", state.message());
    }
    // End of Code by Shriram
}
