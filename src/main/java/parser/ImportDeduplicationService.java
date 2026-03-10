/*
 * Class: ImportDeduplicationService
 * Created by: Person 1
 * Description: Validates import input and determines whether the file content is new or already imported.
 * Example: service.prepare(path, hash -> dao.existsByHash(hash))
 */
package parser;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public class ImportDeduplicationService {
    private final FileHashService fileHashService;

    public ImportDeduplicationService() {
        this(new FileHashService());
    }

    public ImportDeduplicationService(FileHashService fileHashService) {
        this.fileHashService = fileHashService;
    }

    public ImportPreparationResult prepare(Path file, ImportHashLookup hashLookup) throws IOException, SQLException {
        // Guidance:
        // 1. Validate file path exists and is a regular file.
        // 2. Compute file hash via FileHashService.
        // 3. Query hashLookup for duplicate status.
        // 4. Return DUPLICATE result if found, otherwise READY result.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
