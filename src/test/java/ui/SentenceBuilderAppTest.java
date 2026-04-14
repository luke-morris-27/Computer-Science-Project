package ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
    @DisplayName("Ghost stays hidden when draft does not end with whitespace")
    void ghostStaysHiddenWhenDraftDoesNotEndWithWhitespace() {
        // sammy 4/14: keeps the preview hidden after tab or typing until the user presses space for the next word.
        assertFalse(SentenceBuilderApp.shouldShowDraftGhostSuggestion("hello", 5, "world"));
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

    @Test
    @DisplayName("Tab-style append leaves a trailing space")
    void tabStyleAppendLeavesATrailingSpace() {
        // sammy 4/14: proves the tab flow now creates the real separator space that ghost text waits for.
        assertEquals("hello world ", SentenceBuilderApp.buildDraftAfterAppendingWord("hello", "world", true));
    }

    @Test
    @DisplayName("Click-style append does not leave a trailing space")
    void clickStyleAppendDoesNotLeaveATrailingSpace() {
        // sammy 4/14: keeps normal click acceptance behavior unchanged while tab gets the extra trailing space.
        assertEquals("hello world", SentenceBuilderApp.buildDraftAfterAppendingWord("hello", "world", false));
    }

    @Test
    @DisplayName("Ghost stays on the current line when it still fits")
    void ghostStaysOnTheCurrentLineWhenItStillFits() {
        // sammy 4/14: confirms the fit check does not wrap the ghost word early while there is still room beside the caret.
        assertFalse(SentenceBuilderApp.shouldWrapGhostAfterCaret(90.0, 20.0, 120.0));
    }

    @Test
    @DisplayName("Ghost wraps when it would run past the right edge")
    void ghostWrapsWhenItWouldRunPastTheRightEdge() {
        // sammy 4/14: confirms the fit check moves the ghost suggestion down before it overlaps the end of the line.
        assertTrue(SentenceBuilderApp.shouldWrapGhostAfterCaret(100.0, 25.0, 120.0));
    }
}
