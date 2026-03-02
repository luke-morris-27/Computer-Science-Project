package integration;

/*
 * Class: ImportTrackingIT
 * Created by: Archisha Sasson
 * Description: Scaffolds ParserDB import-tracking tests for duplicate file
 * prevention once file hashing is implemented in production code.
 * Example: Importing the same file twice should be rejected by file_hash.
 */

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Code by Archisha Sasson
@Disabled("ParserDB import hashing and duplicate-import prevention are not implemented in production code yet.")
@Tag("integration")
@Tag("parserdb")
@Tag("import-tracking")
@DisplayName("ParserDB Import Tracking Integration Tests")
public class ImportTrackingIT {
    @Test
    @DisplayName("Duplicate file imports are rejected once file-hash tracking is implemented")
    void duplicateFileImportsAreRejectedOnceHashTrackingExists() {
        throw new UnsupportedOperationException("Enable this test after imports.file_hash is wired into the parser import flow.");
    }
}
// End of Code by Archisha Sasson
