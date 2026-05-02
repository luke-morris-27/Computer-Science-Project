package generator;

/*
 * Class: WeightedGeneratorIT
 * Created by: Archisha Sasson
 * Description: Verifies weighted generator behavior against the database,
 * including generated sentence persistence and safe fallback for missing starts.
 * Example: A missing start word should fall back to a valid start word and save the sentence.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Code by Archisha Sasson
@Tag("integration")
@Tag("generator")
@Tag("weighted-generator")
@DisplayName("Weighted Generator Integration Tests")
public class WeightedGeneratorIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Weighted generator saves the sentence and uses a fallback start word")
    void weightedGenerationStoresSentencesAndHandlesMissingStartWords() throws Exception {
        int alphaId = insertWord("alpha", 1);
        int betaId = insertWord("beta", 0);
        insertTransition(alphaId, betaId, 2);

        WeightedGenerator generator = new WeightedGenerator(new Random(0));

        String sentence = generator.generateWeighted("missing", 5);

        assertEquals("missing alpha beta", sentence,
            "Unknown start words are kept, then generation continues from the fallback start word");
        assertEquals(
            1,
            queryForInt(
                "SELECT COUNT(*) FROM generated_sentences WHERE sentence_text = ? AND algorithm_name = ?",
                "missing alpha beta",
                "weighted"
            ),
            "Expected the weighted sentence to be stored with the weighted algorithm label"
        );
        assertEquals(
            alphaId,
            queryForInt(
                "SELECT starting_word_id FROM generated_sentences WHERE sentence_text = ?",
                "missing alpha beta"
            ),
            "Expected the stored sentence to keep the chosen starting word ID"
        );
    }

    @Test
    @DisplayName("Weighted generator returns an empty result when the database has no start word")
    void weightedGenerationReturnsEmptyStringWhenNoValidStartWordsExist() throws Exception {
        WeightedGenerator generator = new WeightedGenerator(new Random(0));

        String sentence = generator.generateWeighted(null, 5);

        assertEquals("", sentence, "Expected generation to return an empty string when the DB has no start words");
        assertEquals(
            0,
            queryForInt("SELECT COUNT(*) FROM generated_sentences"),
            "Expected no generated sentence rows when generation could not start"
        );
    }

    private int insertWord(String wordText, int startCount) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO words (word_text, start_count) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            ps.setString(1, wordText);
            ps.setInt(2, startCount);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertTransition(int fromWordId, int toWordId, int transitionCount) throws Exception {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end) " +
                     "VALUES (?, ?, ?, FALSE, TRUE)"
             )) {
            ps.setInt(1, fromWordId);
            ps.setInt(2, toWordId);
            ps.setInt(3, transitionCount);
            ps.executeUpdate();
        }
    }
}
// End of Code by Archisha Sasson
