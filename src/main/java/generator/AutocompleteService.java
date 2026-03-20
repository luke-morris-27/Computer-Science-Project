/*
 * Class: AutocompleteService
 * Created by: Person 4
 * Description: Applies autocomplete rules for trigger characters, word normalization, suggestions, and unknown-word registration.
 * Example: List<WeightedWord> suggestions = service.suggestNextWords("hello", 5)
 */
package generator;

import java.sql.SQLException;
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
        // Guidance:
        // Return true only for space or comma commit characters.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<WeightedWord> suggestNextWords(String committedWord, int limit) throws SQLException {
        // Guidance:
        // 1. Validate limit > 0.
        // 2. Normalize committedWord.
        // 3. Return empty list for blank normalized words.
        // 4. Query gateway for weighted suggestions.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void registerUnknownWord(String rawWord) throws SQLException {
        // Guidance:
        // 1. Normalize input.
        // 2. Ignore blank normalized values.
        // 3. Delegate insertion/check to gateway.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
