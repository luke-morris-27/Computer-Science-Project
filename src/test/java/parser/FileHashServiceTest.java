package parser;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/*
 * Tests for Task 1, Person 1.
 */
@Tag("unit")
@Tag("task1-person1")
@DisplayName("File Hash Service Tests")
class FileHashServiceTest {
    private final FileHashService hashService = new FileHashService();

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies file hashing behavior.");
    }

    @Test
    @DisplayName("Identical file content yields identical SHA-256")
    void sameContentSameHash() throws Exception {
        Path fileA = Files.createTempFile("hash-a", ".txt");
        Path fileB = Files.createTempFile("hash-b", ".txt");
        Files.writeString(fileA, "sentence builder");
        Files.writeString(fileB, "sentence builder");

        assertEquals(hashService.sha256(fileA), hashService.sha256(fileB));
    }

    @Test
    @DisplayName("Different file content yields different SHA-256")
    void differentContentDifferentHash() throws Exception {
        Path fileA = Files.createTempFile("hash-a", ".txt");
        Path fileB = Files.createTempFile("hash-b", ".txt");
        Files.writeString(fileA, "alpha");
        Files.writeString(fileB, "beta");

        assertNotEquals(hashService.sha256(fileA), hashService.sha256(fileB));
    }

    @Test
    @DisplayName("Empty file still yields a valid 64-char hash")
    void emptyFileHashLengthIsValid() throws Exception {
        Path file = Files.createTempFile("hash-empty", ".txt");
        Files.writeString(file, "");

        assertEquals(64, hashService.sha256(file).length());
    }
}
