package ui;

import java.util.List;
import java.util.Map;
//Code by Archisha Sasson
import java.util.Random;
//End of Code by Archisha Sasson

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import generator.AutocompleteService;
//Code by Archisha Sasson
import generator.WeightedGenerator;
//End of Code by Archisha Sasson
import generator.WeightedWord;
//Code by Archisha Sasson
import parser.Normalizer;
//End of Code by Archisha Sasson

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: AutocompleteControllerTest
 * Created by: Archisha Sasson
 * Description: Verifies autocomplete controller outcomes for suggestions, skipped triggers, blank input, and no-result states.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Autocomplete Controller Tests")
class AutocompleteControllerTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies autocomplete controller ui state behavior.");
    }

    @Test
    @DisplayName("Commit on space requests suggestions in service order")
    void commitOnSpaceRequestsSuggestions() throws Exception {
        //Code by Archisha Sasson
        AutocompleteService service = new AutocompleteService(new FakeGateway(Map.of(
            "hello", List.of(
                new WeightedWord(1, "world", 3),
                new WeightedWord(2, "there", 2)
            )
        )), new Normalizer(), new WeightedGenerator(new FixedRandom(0)));
        //End of Code by Archisha Sasson
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", ' ', 5);

        assertEquals(AutocompleteViewState.AutocompleteOutcome.SHOW_RESULTS, state.outcome());
        assertTrue(state.suggestionsRequested());
        assertTrue(state.hasSuggestions());
        assertEquals(List.of("world", "there"), state.suggestions());
        assertEquals("Loaded 2 suggestions after 'hello'.", state.feedbackMessage());
    }

    @Test
    @DisplayName("Commit on period does not request suggestions")
    void commitOnPeriodDoesNotRequestSuggestions() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway(Map.of(
            "hello", List.of(new WeightedWord(1, "world", 3))
        )));
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", '.', 5);

        assertEquals(AutocompleteViewState.AutocompleteOutcome.SKIPPED_TRIGGER, state.outcome());
        assertFalse(state.suggestionsRequested());
        assertEquals(List.of(), state.suggestions());
        assertEquals("Autocomplete skipped because '.' is not a trigger character.", state.feedbackMessage());
    }

    @Test
    @DisplayName("Blank committed word returns blank-input state")
    void blankCommittedWordReturnsBlankInputState() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway(Map.of()));
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("   ", ' ', 5);

        assertEquals(AutocompleteViewState.AutocompleteOutcome.BLANK_INPUT, state.outcome());
        assertFalse(state.suggestionsRequested());
        assertFalse(state.hasSuggestions());
        assertEquals(List.of(), state.suggestions());
        assertEquals("There is no word available to request suggestions from yet.", state.feedbackMessage());
    }

    @Test
    @DisplayName("Known trigger with no follow-up suggestions returns no-results state")
    void knownTriggerWithNoSuggestionsReturnsNoResultsState() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway(Map.of(
            "hello", List.of()
        )));
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", ' ', 5);

        assertEquals(AutocompleteViewState.AutocompleteOutcome.NO_RESULTS, state.outcome());
        assertTrue(state.suggestionsRequested());
        assertFalse(state.hasSuggestions());
        assertEquals(List.of(), state.suggestions());
        assertEquals("No suggestions found after 'hello'.", state.feedbackMessage());
    }

    private static final class FakeGateway implements generator.AutocompleteGateway {
        private final Map<String, List<WeightedWord>> suggestionsByWord;

        private FakeGateway(Map<String, List<WeightedWord>> suggestionsByWord) {
            this.suggestionsByWord = suggestionsByWord;
        }

        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) {
            return suggestionsByWord.getOrDefault(normalizedWord, List.of());
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            // no-op for unit test
        }
    }

    //Code by Archisha Sasson
    private static final class FixedRandom extends Random {
        private final int nextValue;

        private FixedRandom(int nextValue) {
            this.nextValue = nextValue;
        }

        @Override
        public int nextInt(int bound) {
            return nextValue;
        }
    }
    //End of Code by Archisha Sasson
}
