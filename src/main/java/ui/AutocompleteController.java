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
    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    public AutocompleteViewState onWordCommitted(String committedWord, char commitChar, int limit) throws SQLException {
        if (!autocompleteService.shouldQuerySuggestions(commitChar)) {
            return new AutocompleteViewState(false, List.of());
        }

        List<WeightedWord> weightedSuggestions = autocompleteService.suggestNextWords(committedWord, limit);
        List<String> suggestions = weightedSuggestions.stream()
            .map(WeightedWord::wordText)
            .toList();
        return new AutocompleteViewState(true, suggestions);
    }

    public void registerUserWord(String rawWord) throws SQLException {
        autocompleteService.registerUnknownWord(rawWord);
    }
}
