package generator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import db.AutocompleteDao;
import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: AutocompleteDaoIT
 * Created by: Archisha Sasson
 * Description: Verifies database-backed autocomplete suggestions, ordering, and ensure-word behavior.
 */
@Tag("integration")
@Tag("task2-person4")
@DisplayName("Autocomplete DAO Integration Tests")
class AutocompleteDaoIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Lookup returns next words sorted by transition frequency")
    void lookupReturnsWeightedSuggestions() throws Exception {
        int fromId = insertWord("hello");
        int worldId = insertWord("world");
        int thereId = insertWord("there");
        insertTransition(fromId, worldId, 2);
        insertTransition(fromId, thereId, 5);

        AutocompleteDao dao = new AutocompleteDao();
        List<WeightedWord> suggestions = dao.findNextWordSuggestions("hello", 10);

        assertEquals(2, suggestions.size());
        assertEquals("there", suggestions.get(0).wordText());
        assertEquals(5, suggestions.get(0).weight());
        assertEquals("world", suggestions.get(1).wordText());
    }

    // Code by Shriram
    @Test
    @DisplayName("Tied transition counts are broken alphabetically")
    void tiedCountsOrderedAlphabetically() throws Exception {
        int fromId = insertWord("hello");
        int zebraId = insertWord("zebra");
        int appleId = insertWord("apple");
        insertTransition(fromId, zebraId, 3);
        insertTransition(fromId, appleId, 3);

        AutocompleteDao dao = new AutocompleteDao();
        List<WeightedWord> suggestions = dao.findNextWordSuggestions("hello", 10);

        assertEquals(2, suggestions.size());
        assertEquals("apple", suggestions.get(0).wordText());
        assertEquals("zebra", suggestions.get(1).wordText());
    }
    // End of Code by Shriram

    @Test
    @DisplayName("ensureWordExists inserts missing words")
    void ensureWordExistsInsertsMissingWord() throws Exception {
        AutocompleteDao dao = new AutocompleteDao();
        dao.ensureWordExists("newword");

        assertEquals(1, queryForInt("SELECT COUNT(*) FROM words WHERE word_text = ?", "newword"));
        assertEquals(
            1,
            queryForInt(
                "SELECT COUNT(*) FROM user_input_words uiw " +
                    "JOIN words w ON w.word_id = uiw.word_id " +
                    "WHERE w.word_text = ?",
                "newword"
            )
        );
    }

    private int insertWord(String wordText) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO words (word_text) VALUES (?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            ps.setString(1, wordText);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertTransition(int fromId, int toId, int transitionCount) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end) " +
                     "VALUES (?, ?, ?, FALSE, FALSE)"
             )) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setInt(3, transitionCount);
            ps.executeUpdate();
        }
    }
}
