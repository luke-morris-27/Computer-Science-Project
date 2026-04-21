/*
 * Class: AutocompleteService
 * Created by: Sammy
 * Description: Applies autocomplete rules for trigger characters, word normalization, suggestions, and unknown-word registration.
 * Example: List<WeightedWord> suggestions = service.suggestNextWords("hello", 5)
 */
package generator;

import java.sql.SQLException;
//Code by Archisha Sasson
import java.util.ArrayList;
//End of Code by Archisha Sasson
import java.util.Collections;
import java.util.List;
//Code by Archisha Sasson
import java.util.Random;
//End of Code by Archisha Sasson

import parser.Normalizer;

public class AutocompleteService {
    // talks to the data layer for autocomplete lookups
    private final AutocompleteGateway gateway;

    // cleans user input before lookup or insert
    private final Normalizer normalizer;

    //Code by Archisha Sasson
    // uses the weighted generator's next-word chooser for autocomplete ordering
    private final WeightedGenerator weightedGenerator;
    //End of Code by Archisha Sasson

    // builds the service with a default normalizer
    public AutocompleteService(AutocompleteGateway gateway) {
        this(gateway, new Normalizer());
    }

    // builds the service with injected dependencies
    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer) {
        //Code by Archisha Sasson
        this(gateway, normalizer, new WeightedGenerator(new Random()));
        //End of Code by Archisha Sasson
    }

    //Code by Archisha Sasson
    // builds the service with the weighted generator used to rank suggestions
    public AutocompleteService(AutocompleteGateway gateway, Normalizer normalizer, WeightedGenerator weightedGenerator) {
        this.gateway = gateway;
        this.normalizer = normalizer;
        this.weightedGenerator = weightedGenerator == null ? new WeightedGenerator(new Random()) : weightedGenerator;
    }
    //End of Code by Archisha Sasson

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

        //Code by Archisha Sasson
        return orderSuggestionsWithWeightedGenerator(gateway.findNextWordSuggestions(normalized, limit), limit);
        //End of Code by Archisha Sasson
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

    //Code by Archisha Sasson
    private List<WeightedWord> orderSuggestionsWithWeightedGenerator(List<WeightedWord> suggestions, int limit) {
        if (suggestions == null || suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<WeightedWord> remaining = new ArrayList<>(suggestions);
        List<WeightedWord> ordered = new ArrayList<>();
        while (!remaining.isEmpty() && ordered.size() < limit) {
            WeightedWord chosen = weightedGenerator.chooseWeightedSuggestion(remaining);
            if (chosen == null) {
                break;
            }

            ordered.add(chosen);
            remaining.remove(chosen);
        }
        return ordered;
    }
    //End of Code by Archisha Sasson
}
