package ui;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import generator.AutocompleteService;
import generator.WeightedWord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*
 * Tests for Task 2, Person 5.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Autocomplete Controller Tests")
class AutocompleteControllerTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies autocomplete controller trigger behavior.");
    }

    @Test
    @DisplayName("Commit on space requests suggestions")
    void commitOnSpaceRequestsSuggestions() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway());
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", ' ', 5);

        assertEquals(true, state.suggestionsRequested());
        assertEquals(List.of("world"), state.suggestions());
    }

    @Test
    @DisplayName("Commit on period does not request suggestions")
    void commitOnPeriodDoesNotRequestSuggestions() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway());
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", '.', 5);

        assertFalse(state.suggestionsRequested());
        assertEquals(List.of(), state.suggestions());
    }

    private static final class FakeGateway implements generator.AutocompleteGateway {
        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) {
            return List.of(new WeightedWord(1, "world", 3));
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            // no-op for unit test
        }
    }
}
