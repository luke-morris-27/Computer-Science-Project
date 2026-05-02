/*
 * Class: ImportController
 * Created by: Archisha Sasson
 * Modified by: Shriram
 * Description: Validates import form input and prepares UI-safe messages for import actions.
 * Example: ImportViewState state = controller.validatePath(rawPath)
 */
package ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import db.ImportDao;
import parser.ImportDeduplicationService;
import parser.ImportPreparationResult;
import parser.ImportPreparationStatus;
import parser.WordDb;

public class ImportController {
    // Code by Shriram
    // computes file hashes and decides whether a file has already been imported
    private final ImportDeduplicationService dedupService;

    // remembers the hashes of already-imported files so duplicate imports can be flagged in the preview app
    private final Set<String> importedHashes = new HashSet<>();

    // builds the controller with a default deduplication service
    public ImportController() {
        this(new ImportDeduplicationService());
    }

    // builds the controller with an injected deduplication service so tests can supply a fake hash service
    public ImportController(ImportDeduplicationService dedupService) {
        this.dedupService = dedupService;
    }
    // End of Code by Shriram

    public ImportViewState validatePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return new ImportViewState(false, "Please select a file to import.");
        }

        final Path path;
        try {
            path = Path.of(rawPath.trim());
        } catch (InvalidPathException exception) {
            return new ImportViewState(false, "Selected path is invalid.");
        }

        if (!Files.exists(path)) {
            return new ImportViewState(false, "Selected file does not exist.");
        }

        if (!Files.isRegularFile(path)) {
            return new ImportViewState(false, "Selected path must point to a file.");
        }

        return new ImportViewState(true, "File is ready for import.");
    }

    // Code by Shriram
    // hashes the file and reports whether it has already been imported during this session
    public ImportViewState checkForDuplicate(Path file) {
        try {
            ImportPreparationResult result = dedupService.prepare(file, importedHashes::contains);
            if (result.status() == ImportPreparationStatus.DUPLICATE) {
                return new ImportViewState(false, "This file has already been imported.", true);
            }
            // remembers the hash so a second attempt at the same file is recognized as a duplicate
            importedHashes.add(result.fileHash());
            return new ImportViewState(true, "File is ready for import.");
        } catch (IOException | SQLException exception) {
            return new ImportViewState(false, "Could not read file: " + exception.getMessage());
        }
    }
    // End of Code by Shriram

    public ImportPreparationResult prepareImport(Path file) throws IOException, SQLException {
        try (Connection conn = WordDb.openConnection()) {
            return dedupService.prepare(file, hash -> new ImportDao(conn).existsByHash(hash));
        }
    }
}
