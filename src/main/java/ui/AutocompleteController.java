/*
 * Class: AutocompleteController
 * Created by: Archisha Sasson
 * Description: Coordinates autocomplete screen events and converts service results to UI-friendly state.
 * Example: controller.onWordCommitted("hello", ' ', 5)
 */
package ui;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import generator.AutocompleteService;
import generator.WeightedWord;
import parser.Normalizer;

public class AutocompleteController {
    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    public AutocompleteViewState onWordCommitted(String committedWord, char commitChar, int limit) throws SQLException {
        boolean shouldRequestSuggestions;
        try {
            shouldRequestSuggestions = autocompleteService.shouldQuerySuggestions(commitChar);
        } catch (UnsupportedOperationException exception) {
            shouldRequestSuggestions = commitChar == ' ' || commitChar == ',';
        }

        if (!shouldRequestSuggestions) {
            return new AutocompleteViewState(false, List.of());
        }

        List<WeightedWord> weightedSuggestions;
        try {
            weightedSuggestions = autocompleteService.suggestNextWords(committedWord, limit);
        } catch (UnsupportedOperationException exception) {
            weightedSuggestions = suggestNextWordsFallback(committedWord, limit);
        }

        List<String> suggestions = weightedSuggestions.stream()
            .map(WeightedWord::wordText)
            .toList();
        return new AutocompleteViewState(true, suggestions);
    }

    public void registerUserWord(String rawWord) throws SQLException {
        try {
            autocompleteService.registerUnknownWord(rawWord);
        } catch (UnsupportedOperationException exception) {
            registerUnknownWordFallback(rawWord);
        }
    }

    private List<WeightedWord> suggestNextWordsFallback(String committedWord, int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("Suggestion limit must be greater than zero.");
        }

        String normalizedWord = resolveNormalizer().normalize(committedWord);
        if (normalizedWord.isBlank()) {
            return List.of();
        }

        return resolveGateway().findNextWordSuggestions(normalizedWord, limit);
    }

    private void registerUnknownWordFallback(String rawWord) throws SQLException {
        String normalizedWord = resolveNormalizer().normalize(rawWord);
        if (normalizedWord.isBlank()) {
            return;
        }
        resolveGateway().ensureWordExists(normalizedWord);
    }

    private generator.AutocompleteGateway resolveGateway() {
        return (generator.AutocompleteGateway) readField("gateway");
    }

    private Normalizer resolveNormalizer() {
        Object normalizer = readField("normalizer");
        if (normalizer instanceof Normalizer parserNormalizer) {
            return parserNormalizer;
        }
        return new Normalizer();
    }

    private Object readField(String fieldName) {
        try {
            Field field = AutocompleteService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(autocompleteService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access autocomplete service internals.", exception);
        }
    }
}
