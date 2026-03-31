package generator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: AutocompleteServiceTest
 * Created by: Archisha Sasson
 * Description: Verifies autocomplete service normalization, trigger checks, limit validation, and unknown-word registration.
 */
@Tag("unit")
@Tag("task2-person4")
@DisplayName("Autocomplete Service Tests")
class AutocompleteServiceTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies autocomplete trigger/normalization behavior.");
    }

    @Test
    @DisplayName("Suggestion trigger only activates on space or comma")
    void suggestionTriggerRuleMatchesSpec() {
        AutocompleteService service = new AutocompleteService(new FakeGateway());

        assertTrue(service.shouldQuerySuggestions(' '));
        assertTrue(service.shouldQuerySuggestions(','));
        assertEquals(false, service.shouldQuerySuggestions('.'));
    }

    @Test
    @DisplayName("Committed word is normalized before lookup")
    void normalizesBeforeLookup() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.suggestions = List.of(new WeightedWord(2, "world", 3));
        AutocompleteService service = new AutocompleteService(gateway);

        List<WeightedWord> result = service.suggestNextWords("Hello", 5);

        assertEquals("hello", gateway.lastLookupWord);
        assertEquals(5, gateway.lastLimit);
        assertEquals(1, result.size());
    }

    // Code by Shriram
    @Test
    @DisplayName("Blank input returns empty suggestion list")
    void blankInputReturnsEmpty() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway());

        assertTrue(service.suggestNextWords("   ", 5).isEmpty());
    }

    @Test
    @DisplayName("Null input returns empty suggestion list")
    void nullInputReturnsEmpty() throws Exception {
        AutocompleteService service = new AutocompleteService(new FakeGateway());

        assertTrue(service.suggestNextWords(null, 5).isEmpty());
    }

    @Test
    @DisplayName("Zero limit throws IllegalArgumentException")
    void zeroLimitThrows() {
        AutocompleteService service = new AutocompleteService(new FakeGateway());

        assertThrows(IllegalArgumentException.class, () -> service.suggestNextWords("hello", 0));
    }

    @Test
    @DisplayName("Negative limit throws IllegalArgumentException")
    void negativeLimitThrows() {
        AutocompleteService service = new AutocompleteService(new FakeGateway());

        assertThrows(IllegalArgumentException.class, () -> service.suggestNextWords("hello", -1));
    }
    // End of Code by Shriram

    @Test
    @DisplayName("Unknown word registration normalizes and stores")
    void registerUnknownWordNormalizes() throws Exception {
        FakeGateway gateway = new FakeGateway();
        AutocompleteService service = new AutocompleteService(gateway);

        service.registerUnknownWord("  New-Word! ");

        assertEquals(List.of("new-word"), gateway.registered);
    }

    private static final class FakeGateway implements AutocompleteGateway {
        private List<WeightedWord> suggestions = List.of();
        private final List<String> registered = new ArrayList<>();
        private String lastLookupWord;
        private int lastLimit;

        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException {
            this.lastLookupWord = normalizedWord;
            this.lastLimit = limit;
            return suggestions;
        }

        @Override
        public void ensureWordExists(String normalizedWord) throws SQLException {
            registered.add(normalizedWord);
        }
    }
}
