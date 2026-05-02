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

import db.ImportDao;
import parser.ImportDeduplicationService;
import parser.ImportHashLookup;
import parser.ImportPreparationResult;
import parser.ImportPreparationStatus;
import parser.WordDb;

public class ImportController {
    // Code by Shriram
    // computes file hashes and decides whether a file has already been imported
    private final ImportDeduplicationService dedupService;
    private final ImportHashLookup persistentHashLookup;

    // builds the controller with a default deduplication service
    public ImportController() {
        this(new ImportDeduplicationService(), ImportController::hashExistsInImportsTable);
    }

    // builds the controller with an injected deduplication service so tests can supply a fake hash service
    public ImportController(ImportDeduplicationService dedupService, ImportHashLookup persistentHashLookup) {
        this.dedupService = dedupService;
        this.persistentHashLookup = persistentHashLookup;
    }

    /** For tests or tools that need the raw preparation outcome (including file_hash for insertImport). */
    public ImportPreparationResult prepareImport(Path file) throws IOException, SQLException {
        return dedupService.prepare(file, persistentHashLookup);
    }

    private static boolean hashExistsInImportsTable(String fileHash) throws SQLException {
        try (Connection conn = WordDb.openConnection()) {
            return new ImportDao(conn).existsByHash(fileHash);
        }
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
    // hashes the file and reports whether it has already been recorded in persistent imports
    public ImportViewState checkForDuplicate(Path file) {
        try {
            ImportPreparationResult result = dedupService.prepare(file, persistentHashLookup);
            if (result.status() == ImportPreparationStatus.DUPLICATE) {
                return new ImportViewState(false, "This file has already been imported.", true);
            }
            return new ImportViewState(true, "File is ready for import.");
        } catch (IOException | SQLException exception) {
            return new ImportViewState(false, "Could not read file: " + exception.getMessage());
        }
    }
    // End of Code by Shriram
}
