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
    // Code by Shriram
    // controls how many candidates we pull from the database before weighted selection
    private static final int CANDIDATE_POOL_MULTIPLIER = 4;
    // End of Code by Shriram

    // talks to the data layer for autocomplete lookups
    private final AutocompleteGateway gateway;

    // cleans user input before lookup or insert
    private final Normalizer normalizer;

    // Code by Shriram
    // uses the weighted algorithm class to pick autocomplete suggestions
    private final WeightedGenerator weightedGenerator;

    // remembers the most recent suggestion word so we can return the same result for repeated lookups
    private String cachedWord;

    // remembers the most recent limit alongside the cached word
    private int cachedLimit;

    // remembers the most recent suggestion list so the ghost text and the suggestion list cannot disagree
    private List<WeightedWord> cachedSuggestions;
    // End of Code by Shriram

    // builds the service with a default normalizer
    public AutocompleteService(AutocompleteGateway gateway) {
        this(gateway, new Normalizer(), new WeightedGenerator());
    }

    // builds the service with a custom normalizer and a default weighted generator
    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer) {
        this(gateway, normalizer, new WeightedGenerator());
    }

    // Code by Shriram
    // builds the service with all dependencies injected so weighted selection can be deterministic in tests
    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer, WeightedGenerator weightedGenerator) {
        this.gateway = gateway;
        this.normalizer = normalizer;
        this.weightedGenerator = weightedGenerator;
    }
    // End of Code by Shriram

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

        // Code by Shriram
        // returns the cached result so the ghost text and suggestion list always agree when the same word is queried
        if (normalized.equals(cachedWord) && limit == cachedLimit && cachedSuggestions != null) {
            return cachedSuggestions;
        }

        // pulls a wider candidate pool so weighted selection has meaningful options to choose from
        int poolSize = limit * CANDIDATE_POOL_MULTIPLIER;
        List<WeightedWord> candidates = gateway.findNextWordSuggestions(normalized, poolSize);

        // delegates to the weighted algorithm class so suggestions reflect frequency-weighted probability
        List<WeightedWord> suggestions = weightedGenerator.pickWeightedSuggestions(candidates, limit);

        // caches the freshly computed result so repeated lookups for the same word stay consistent
        cachedWord = normalized;
        cachedLimit = limit;
        cachedSuggestions = suggestions;

        return suggestions;
        // End of Code by Shriram
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
