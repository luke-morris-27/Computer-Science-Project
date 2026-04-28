package generator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
//Code by Archisha Sasson
import java.util.Random;
//End of Code by Archisha Sasson

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import parser.Normalizer;

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
        // pool multiplier is 4 so the gateway is asked for 5 * 4 = 20 candidates
        assertEquals(20, gateway.lastLimit);
        assertEquals(1, result.size());
    }

    //Code by Archisha Sasson
    @Test
    @DisplayName("Suggestions are ordered using weighted generator selection")
    void suggestionsUseWeightedGeneratorSelection() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.suggestions = List.of(
            new WeightedWord(1, "apple", 1),
            new WeightedWord(2, "zebra", 9)
        );
        WeightedGenerator weightedGenerator = new WeightedGenerator(new FixedRandom(1));
        AutocompleteService service = new AutocompleteService(gateway, new Normalizer(), weightedGenerator);

        List<WeightedWord> result = service.suggestNextWords("hello", 1);

        assertEquals(List.of("zebra"), result.stream().map(WeightedWord::wordText).toList());
    }
    //End of Code by Archisha Sasson

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
