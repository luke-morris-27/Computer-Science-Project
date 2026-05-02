package generator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import support.DatabaseIntegrationTestSupport;

@Tag("integration")
@Tag("generator")
@Tag("random-generator")
@DisplayName("Random Generator Integration Tests")
public class RandomGeneratorIT extends DatabaseIntegrationTestSupport {

    @Test
    @DisplayName("Random generator saves the sentence and uses a fallback start word")
    void randomGenerationStoresSentencesAndHandlesMissingStartWords() throws Exception {
        int alphaId = insertWord("alpha", 1);
        int betaId = insertWord("beta", 0);
        insertTransition(alphaId, betaId, 2);

        RandomGenerator generator = new RandomGenerator(
            new db.DbGeneratorRepository(),
            new Random(0),
            new parser.Normalizer()
        );

        String sentence = generator.generateRandom("missing", 5);

        assertEquals("alpha beta", sentence);
        assertEquals(
            1,
            queryForInt(
                "SELECT COUNT(*) FROM generated_sentences WHERE sentence_text = ? AND algorithm_name = ?",
                "alpha beta",
                "random"
            )
        );
        assertEquals(
            alphaId,
            queryForInt(
                "SELECT starting_word_id FROM generated_sentences WHERE sentence_text = ?",
                "alpha beta"
            )
        );
    }

    @Test
    @DisplayName("Random generator returns an empty result when the database has no start word")
    void randomGenerationReturnsEmptyStringWhenNoValidStartWordsExist() throws Exception {
        RandomGenerator generator = new RandomGenerator(
            new db.DbGeneratorRepository(),
            new Random(0),
            new parser.Normalizer()
        );

        String sentence = generator.generateRandom(null, 5);

        assertEquals("", sentence);
        assertEquals(0, queryForInt("SELECT COUNT(*) FROM generated_sentences"));
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