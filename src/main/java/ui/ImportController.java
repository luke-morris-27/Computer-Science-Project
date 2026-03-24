/*
 * Class: ImportController
 * Created by: Archisha Sasson
 * Description: Validates import form input and prepares UI-safe messages for import actions.
 * Example: ImportViewState state = controller.validatePath(rawPath)
 */
package ui;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ImportController {
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
}
