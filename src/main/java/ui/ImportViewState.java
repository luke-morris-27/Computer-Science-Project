/*
 * Class: ImportViewState
 * Created by: Archisha Sasson
 * Modified by: Shriram
 * Description: Encapsulates import-screen validation outcome and user-facing message.
 * Example: new ImportViewState(false, "Selected file does not exist")
 */
package ui;

public record ImportViewState(boolean valid, String message, boolean duplicate) {
    // Code by Shriram
    // keeps the original two-argument shape working so existing callers do not need to change
    public ImportViewState(boolean valid, String message) {
        this(valid, message, false);
    }
    // End of Code by Shriram
}
