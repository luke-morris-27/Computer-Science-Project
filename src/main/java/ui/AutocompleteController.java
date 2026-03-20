/*
 * Class: AutocompleteController
 * Created by: Person 5
 * Description: Coordinates autocomplete screen events and converts service results to UI-friendly state.
 * Example: controller.onWordCommitted("hello", ' ', 5)
 */
package ui;

import java.sql.SQLException;

import generator.AutocompleteService;

public class AutocompleteController {
    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    public AutocompleteViewState onWordCommitted(String committedWord, char commitChar, int limit) throws SQLException {
        // Guidance:
        // 1. Check trigger character via autocompleteService.
        // 2. Request suggestions when allowed.
        // 3. Map weighted suggestions to plain word list for UI display.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void registerUserWord(String rawWord) throws SQLException {
        // Guidance:
        // Forward unknown-word registration to autocompleteService.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
