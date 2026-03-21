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
    private final AutocompleteGateway gateway;
    private final Normalizer normalizer;

    public AutocompleteService(AutocompleteGateway gateway) {
        this(gateway, new Normalizer());
    }

    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer) {
        this.gateway = gateway;
        this.normalizer = normalizer;
    }

    public boolean shouldQuerySuggestions(char committedChar) {
        return committedChar == ' ' || committedChar == ',';
    }

    public List<WeightedWord> suggestNextWords(String committedWord, int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }

        String normalized = normalize(committedWord);
        if (normalized.isBlank()) {
            return Collections.emptyList();
        }

        return gateway.findNextWordSuggestions(normalized, limit);
    }

    public void registerUnknownWord(String rawWord) throws SQLException {
        String normalized = normalize(rawWord);
        if (normalized.isBlank()) {
            return;
        }

        gateway.ensureWordExists(normalized);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }

        String normalized = normalizer.normalize(input);
        return normalized == null ? "" : normalized.trim();
    }
}
