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
 * Tests for Task 2, Person 5.
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
}
