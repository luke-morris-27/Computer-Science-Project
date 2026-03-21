/*
 * Class: AutocompleteController
 * Created by: Archisha Sasson
 * Description: Coordinates autocomplete screen events and converts service results to UI-friendly state.
 * Example: controller.onWordCommitted("hello", ' ', 5)
 */
package ui;

import java.sql.SQLException;
import java.util.List;

import generator.AutocompleteService;
import generator.WeightedWord;

public class AutocompleteController {
    // talks to the autocomplete service for ui requests
    private final AutocompleteService autocompleteService;

    // builds the controller with an autocomplete service
    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    // handles a committed word and returns suggestion state for the ui
    public AutocompleteViewState onWordCommitted(String committedWord, char commitChar, int limit) throws SQLException {
        if (!autocompleteService.shouldQuerySuggestions(commitChar)) {
            return new AutocompleteViewState(false, List.of());
        }

        List<WeightedWord> weightedSuggestions = autocompleteService.suggestNextWords(committedWord, limit);

        // converts weighted results into plain words for the ui
        List<String> suggestions = weightedSuggestions.stream()
            .map(WeightedWord::wordText)
            .toList();
        return new AutocompleteViewState(true, suggestions);
    }

    // sends a new user word to the service
    public void registerUserWord(String rawWord) throws SQLException {
        autocompleteService.registerUnknownWord(rawWord);
    }
}
