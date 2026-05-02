package ui;

//Code by Archisha Sasson
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;
//End of Code by Archisha Sasson

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

//Code by Archisha Sasson
import generator.AutocompleteGateway;
import generator.AutocompleteService;
import generator.WeightedGenerator;
import generator.WeightedWord;
import parser.Normalizer;
//End of Code by Archisha Sasson

/*
 * Class: SentenceBuilderAppTest
 * Created by: Sammy 4/14
 * Description: Verifies the simple caret-at-end rule that controls whether draft ghost text should be allowed to show.
 */
@Tag("unit")
@DisplayName("SentenceBuilderApp caret rule tests")
class SentenceBuilderAppTest {
    @Test
    @DisplayName("Caret at draft end returns true")
    void caretAtDraftEndReturnsTrue() {
        // sammy 4/14: checks the normal case where the caret is exactly at the end of the typed draft.
        assertTrue(SentenceBuilderApp.isCaretAtDraftEnd("hello world", 11));
    }

    @Test
    @DisplayName("Caret in the middle returns false")
    void caretInMiddleReturnsFalse() {
        // sammy 4/14: proves the ghost text should stay hidden when the user clicks back into the middle of the sentence.
        assertFalse(SentenceBuilderApp.isCaretAtDraftEnd("hello world", 5));
    }

    @Test
    @DisplayName("Caret beyond text is clamped to end")
    void caretBeyondTextIsClampedToEnd() {
        // sammy 4/14: keeps the helper forgiving if JavaFX ever reports a caret position slightly past the text length.
        assertTrue(SentenceBuilderApp.isCaretAtDraftEnd("hello", 99));
    }

    @Test
    @DisplayName("Ghost shows when draft ends with whitespace and caret is at end")
    void ghostShowsWhenDraftEndsWithWhitespaceAndCaretIsAtEnd() {
        // sammy 4/14: proves the new simpler rule only shows ghost text after the user has typed a real trailing space.
        assertTrue(SentenceBuilderApp.shouldShowDraftGhostSuggestion("hello ", 6, "world"));
    }

    @Test
    @DisplayName("Ghost shows at the end of the last typed word")
    void ghostShowsAtEndOfLastTypedWord() {
        //Code by Archisha Sasson
        // The inline ghost can supply its own leading space when the caret is after the last word.
        assertTrue(SentenceBuilderApp.shouldShowDraftGhostSuggestion("hello", 5, "world"));
        //End of Code by Archisha Sasson
    }

    @Test
    @DisplayName("Ghost stays hidden when caret is not at end even with whitespace")
    void ghostStaysHiddenWhenCaretIsNotAtEndEvenWithWhitespace() {
        // sammy 4/14: keeps the preview off while the user edits in the middle even if a trailing space exists elsewhere.
        assertFalse(SentenceBuilderApp.shouldShowDraftGhostSuggestion("hello ", 3, "world"));
    }

    @Test
    @DisplayName("Ghost stays hidden when there is no suggestion")
    void ghostStaysHiddenWhenThereIsNoSuggestion() {
        // sammy 4/14: keeps the visibility rule safe when autocomplete has not produced a real next word yet.
        assertFalse(SentenceBuilderApp.shouldShowDraftGhostSuggestion("hello ", 6, ""));
    }

    //Code by Archisha Sasson
    @Test
    @DisplayName("Ghost only shows at the last word boundary")
    void ghostOnlyShowsAtLastWordBoundary() {
        assertTrue(SentenceBuilderApp.isAtLastWordBoundary("hello", 5));
        assertTrue(SentenceBuilderApp.isAtLastWordBoundary("hello ", 6));
        assertTrue(SentenceBuilderApp.isAtLastWordBoundary("hello world", 11));
        assertFalse(SentenceBuilderApp.isAtLastWordBoundary("hello  ", 7));
        assertFalse(SentenceBuilderApp.isAtLastWordBoundary("hello\n", 6));
        assertFalse(SentenceBuilderApp.isAtLastWordBoundary("hello world ", 5));
    }
    //End of Code by Archisha Sasson

    //Code by Archisha Sasson
    @Test
    @DisplayName("Ghost suggestion uses weighted autocomplete algorithm")
    void ghostSuggestionUsesWeightedAutocompleteAlgorithm() throws Exception {
        AutocompleteService service = new AutocompleteService(
            new FakeAutocompleteGateway(Map.of(
                "hello",
                List.of(
                    new WeightedWord(1, "apple", 1),
                    new WeightedWord(2, "zebra", 9)
                )
            )),
            new Normalizer(),
            new WeightedGenerator(new FixedRandom(1))
        );
        AutocompleteController controller = new AutocompleteController(service);

        AutocompleteViewState state = controller.onWordCommitted("hello", ' ', 1);

        assertEquals("zebra", SentenceBuilderApp.selectDraftGhostSuggestion(state));
    }

    @Test
    @DisplayName("Ghost preview shows only the inline suggestion text")
    void ghostPreviewShowsOnlyTheInlineSuggestionText() {
        assertEquals(" little", SentenceBuilderApp.buildDraftGhostSuggestion("I am not a", "little"));
        assertEquals("little", SentenceBuilderApp.buildDraftGhostSuggestion("I am not a ", "little"));
    }

    //Code by Archisha Sasson
    @Test
    @DisplayName("Ghost placement uses the caret line when there is room")
    void ghostPlacementUsesCaretLineWhenThereIsRoom() {
        SentenceBuilderApp.DraftGhostPlacement placement =
            SentenceBuilderApp.calculateDraftGhostPlacement(100, 20, 18, 60, 18, 300, 120);

        assertEquals(102, placement.x());
        assertEquals(20, placement.y());
    }

    @Test
    @DisplayName("Ghost placement hides when the caret line is full")
    void ghostPlacementHidesWhenCaretLineIsFull() {
        SentenceBuilderApp.DraftGhostPlacement placement =
            SentenceBuilderApp.calculateDraftGhostPlacement(260, 20, 18, 60, 18, 300, 120);

        assertNull(placement);
    }

    @Test
    @DisplayName("Ghost placement hides when there is no safe same-line space")
    void ghostPlacementHidesWhenThereIsNoSafeSameLineSpace() {
        SentenceBuilderApp.DraftGhostPlacement placement =
            SentenceBuilderApp.calculateDraftGhostPlacement(260, 82, 18, 60, 18, 300, 110);

        assertNull(placement);
    }
    //End of Code by Archisha Sasson

    @Test
    @DisplayName("Tab-style append leaves a trailing space")
    void tabStyleAppendLeavesATrailingSpace() {
        // sammy 4/14: proves the tab flow now creates the real separator space that ghost text waits for.
        assertEquals("hello world ", SentenceBuilderApp.buildDraftAfterAppendingWord("hello", "world", true));
        //Code by Archisha Sasson
        assertEquals("I am not a little ", SentenceBuilderApp.buildDraftAfterAppendingWord("I am not a", "little", true));
        //End of Code by Archisha Sasson
    }

    @Test
    @DisplayName("Click-style append does not leave a trailing space")
    void clickStyleAppendDoesNotLeaveATrailingSpace() {
        // sammy 4/14: keeps normal click acceptance behavior unchanged while tab gets the extra trailing space.
        assertEquals("hello world", SentenceBuilderApp.buildDraftAfterAppendingWord("hello", "world", false));
    }

    //Code by Archisha Sasson
    private static final class FakeAutocompleteGateway implements AutocompleteGateway {
        private final Map<String, List<WeightedWord>> suggestionsByWord;

        private FakeAutocompleteGateway(Map<String, List<WeightedWord>> suggestionsByWord) {
            this.suggestionsByWord = suggestionsByWord;
        }

        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException {
            return suggestionsByWord.getOrDefault(normalizedWord, List.of());
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            // no-op for unit test
        }
    }

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
