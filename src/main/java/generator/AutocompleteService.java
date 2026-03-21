/*
 * Class: AutocompleteService
 * Created by: Sammy
 * Description: Applies autocomplete rules for trigger characters, word normalization, suggestions, and unknown-word registration.
 * Example: List<WeightedWord> suggestions = service.suggestNextWords("hello", 5)
 */
package generator;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import parser.Normalizer;

public class AutocompleteService {
    // talks to the data layer for autocomplete lookups
    private final AutocompleteGateway gateway;

    // cleans user input before lookup or insert
    private final Normalizer normalizer;

    // builds the service with a default normalizer
    public AutocompleteService(AutocompleteGateway gateway) {
        this(gateway, new Normalizer());
    }

    // builds the service with injected dependencies
    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer) {
        this.gateway = gateway;
        this.normalizer = normalizer;
    }

    // only triggers suggestions after space or comma
    public boolean shouldQuerySuggestions(char committedChar) {
        return committedChar == ' ' || committedChar == ',';
    }

    // gets autocomplete suggestions for a word they already typed
    public List<WeightedWord> suggestNextWords(String committedWord, int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }

        // stores the cleaned version of the typed word
        String normalized = normalize(committedWord);
        if (normalized.isBlank()) {
            return Collections.emptyList();
        }

        return gateway.findNextWordSuggestions(normalized, limit);
    }

    // stores a new word if the user typed one we do not know yet
    public void registerUnknownWord(String rawWord) throws SQLException {
        // stores the cleaned version of the new word
        String normalized = normalize(rawWord);
        if (normalized.isBlank()) {
            return;
        }

        gateway.ensureWordExists(normalized);
    }

    // cleans input and returns a safe string
    private String normalize(String input) {
        if (input == null) {
            return "";
        }

        // stores the cleaned version of the input
        String normalized = normalizer.normalize(input);
        return normalized == null ? "" : normalized.trim();
    }
}
