/*
 * Class: ImportDeduplicationService
 * Created by: Omesh Sana
 * Description: Validates import input and determines whether the file content is new or already imported.
 * Example: service.prepare(path, hash -> dao.existsByHash(hash))
 */
package parser;

import java.io.IOException;
import java.nio.file.Files;
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

    public ImportPreparationResult prepare(Path file, ImportHashLookup hashLookup)
            throws IOException, SQLException {

        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file must exist and be a regular file: " + file);
        }

        // Compute hash for this file
        String hash = fileHashService.sha256(file);

        // Check if this hash already exists
        boolean exists = hashLookup.existsByHash(hash);

        if (exists) {
            return new ImportPreparationResult(
                    ImportPreparationStatus.DUPLICATE,
                    hash,
                    "File has already been imported"
            );
        } else {
            return new ImportPreparationResult(
                    ImportPreparationStatus.READY,
                    hash,
                    "File is new and ready for import"
            );
        }
    }
}
