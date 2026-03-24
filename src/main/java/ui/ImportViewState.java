/*
 * Class: ImportViewState
 * Created by: Archisha Sasson
 * Description: Encapsulates import-screen validation outcome and user-facing message.
 * Example: new ImportViewState(false, "Selected file does not exist")
 */
package ui;

public record ImportViewState(boolean valid, String message) {
}
