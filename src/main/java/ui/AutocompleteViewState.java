/*
 * Class: AutocompleteViewState
 * Created by: Archisha Sasson
 * Description: Encapsulates whether suggestions were requested and what suggestions should be shown.
 * Example: new AutocompleteViewState(true, List.of("world", "there"))
 */
package ui;

import java.util.List;

public record AutocompleteViewState(
    AutocompleteOutcome outcome,
    String feedbackMessage,
    List<String> suggestions
) {
    // sammy 3/30: stores a safe copy of the message and suggestions so the ui can reuse this state without defensive checks.
    public AutocompleteViewState {
        outcome = outcome == null ? AutocompleteOutcome.BLANK_INPUT : outcome;
        feedbackMessage = feedbackMessage == null ? "" : feedbackMessage;
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    // sammy 3/30: tells the ui whether autocomplete actually ran a lookup for the committed word.
    public boolean suggestionsRequested() {
        return outcome == AutocompleteOutcome.SHOW_RESULTS || outcome == AutocompleteOutcome.NO_RESULTS;
    }

    // sammy 3/30: gives the ui a clean way to know when there is anything worth rendering in the list.
    public boolean hasSuggestions() {
        return outcome == AutocompleteOutcome.SHOW_RESULTS && !suggestions.isEmpty();
    }

    // sammy 3/30: builds the state used when a key like period should not open autocomplete suggestions.
    public static AutocompleteViewState skippedTrigger(char commitChar) {
        return new AutocompleteViewState(
            AutocompleteOutcome.SKIPPED_TRIGGER,
            "Autocomplete skipped because '" + commitChar + "' is not a trigger character.",
            List.of()
        );
    }

    // sammy 3/30: builds the state used when the ui does not have a usable committed word yet.
    public static AutocompleteViewState blankInput() {
        return new AutocompleteViewState(
            AutocompleteOutcome.BLANK_INPUT,
            "There is no word available to request suggestions from yet.",
            List.of()
        );
    }

    // sammy 3/30: builds the state used when autocomplete ran but the word has no follow-up suggestions.
    public static AutocompleteViewState noResults(String committedWord) {
        return new AutocompleteViewState(
            AutocompleteOutcome.NO_RESULTS,
            "No suggestions found after '" + committedWord + "'.",
            List.of()
        );
    }

    // sammy 3/30: builds the state used when autocomplete found suggestions that should be shown in the list.
    public static AutocompleteViewState showingSuggestions(String committedWord, List<String> suggestions) {
        return new AutocompleteViewState(
            AutocompleteOutcome.SHOW_RESULTS,
            "Loaded " + suggestions.size() + " suggestions after '" + committedWord + "'.",
            suggestions
        );
    }

    // sammy 3/30: names the high-level autocomplete result so the ui can handle each path clearly.
    public enum AutocompleteOutcome {
        SKIPPED_TRIGGER,
        BLANK_INPUT,
        NO_RESULTS,
        SHOW_RESULTS
    }
}
