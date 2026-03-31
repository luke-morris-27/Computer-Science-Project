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
        // sammy 3/30: keeps blank input from looking like a real autocomplete miss in the ui.
        if (committedWord == null || committedWord.isBlank()) {
            return AutocompleteViewState.blankInput();
        }

        if (!autocompleteService.shouldQuerySuggestions(commitChar)) {
            return AutocompleteViewState.skippedTrigger(commitChar);
        }

        List<WeightedWord> weightedSuggestions = autocompleteService.suggestNextWords(committedWord, limit);

        // converts weighted results into plain words for the ui
        List<String> suggestions = weightedSuggestions.stream()
            .map(WeightedWord::wordText)
            .toList();

        // sammy 3/30: tells the ui when a real lookup happened but there were simply no next words to show.
        if (suggestions.isEmpty()) {
            return AutocompleteViewState.noResults(committedWord.trim());
        }

        // sammy 3/30: returns suggestions in the same order supplied by the service layer.
        return AutocompleteViewState.showingSuggestions(committedWord.trim(), suggestions);
    }

    // sends a new user word to the service
    public void registerUserWord(String rawWord) throws SQLException {
        autocompleteService.registerUnknownWord(rawWord);
    }
}
