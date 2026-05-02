package generator;

/*
 * Class: GreedyGeneratorIT
 * Created by: Archisha Sasson
 * Description: Verifies greedy generator behavior against the database,
 * including deterministic next-word selection and generated sentence storage.
 * Example: A missing start word should fall back to the highest-ranked start.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@Tag("generator")
@Tag("greedy-generator")
@DisplayName("Greedy Generator Integration Tests")
public class GreedyGeneratorIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Greedy generator saves the sentence and uses the best start word")
    void greedyGenerationStoresGeneratedSentencesAndFallsBackToTheBestStartWord() throws Exception {
        int alphaId = insertWord("alpha", 1);
        int betaId = insertWord("beta", 3);
        int gammaId = insertWord("gamma", 0);
        insertTransition(betaId, gammaId, 5);
        insertTransition(alphaId, gammaId, 1);

        GreedyGenerator generator = new GreedyGenerator();

        String sentence = generator.generateGreedy("missing", 5);

        assertEquals("missing beta gamma", sentence,
            "Unknown start words are kept, then generation continues from the best DB start word");
        assertEquals(
            1,
            queryForInt(
                "SELECT COUNT(*) FROM generated_sentences WHERE sentence_text = ? AND algorithm_name = ?",
                "missing beta gamma",
                "greedy"
            ),
            "Expected the greedy sentence to be stored with the greedy algorithm label"
        );
        assertEquals(
            betaId,
            queryForInt(
                "SELECT starting_word_id FROM generated_sentences WHERE sentence_text = ?",
                "missing beta gamma"
            ),
            "Expected the stored greedy sentence to keep the chosen starting word ID"
        );
    }

    @Test
    @DisplayName("Greedy generator returns an empty result when the database has no start word")
    void greedyGenerationReturnsAnEmptyStringWhenTheDatabaseHasNoValidStartWords() throws Exception {
        GreedyGenerator generator = new GreedyGenerator();

        String sentence = generator.generateGreedy(null, 5);

        assertEquals("", sentence, "Expected generation to return an empty string when the DB has no start words");
        assertEquals(
            0,
            queryForInt("SELECT COUNT(*) FROM generated_sentences"),
            "Expected no generated sentence rows when greedy generation could not start"
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
