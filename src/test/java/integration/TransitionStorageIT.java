package integration;

/*
 * Class: TransitionStorageIT
 * Created by: Archisha Sasson
 * Description: Verifies ParserDB transition storage behavior including
 * frequency updates and sentence start/end boundary metadata.
 * Example: "Alpha beta. Alpha beta." should increment one alpha->beta row.
 */

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import parser.ImportService;
import parser.Normalizer;
import parser.TextParser;
import parser.Tokenizer;
import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Code by Archisha Sasson
@Tag("integration")
@Tag("parserdb")
@Tag("transition-storage")
@DisplayName("Transition Storage Integration Tests")
public class TransitionStorageIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Repeated word pairs raise the transition count instead of making duplicates")
    void repeatedWordPairsIncrementTransitionFrequency() throws Exception {
        Path inputFile = writeInputFile("repeated-pairs", "Alpha beta. Alpha beta. Alpha beta.");
        ImportService importService = new ImportService(new TextParser(new Tokenizer(), new Normalizer()));

        importService.importFile(inputFile, "transition-storage-" + System.nanoTime());

        assertEquals(
            1,
            queryForInt(
                "SELECT COUNT(*) FROM next_word nw " +
                    "JOIN words w1 ON w1.word_id = nw.from_word_id " +
                    "JOIN words w2 ON w2.word_id = nw.to_word_id " +
                    "WHERE w1.word_text = ? AND w2.word_text = ?",
                "alpha",
                "beta"
            ),
            "Expected only one transition row for the repeated alpha -> beta pair"
        );
        assertEquals(
            3,
            queryForInt(
                "SELECT transition_count FROM next_word nw " +
                    "JOIN words w1 ON w1.word_id = nw.from_word_id " +
                    "JOIN words w2 ON w2.word_id = nw.to_word_id " +
                    "WHERE w1.word_text = ? AND w2.word_text = ?",
                "alpha",
                "beta"
            ),
            "Expected repeated alpha -> beta pairs to increment transition_count to 3"
        );
    }

    @Test
    @DisplayName("Sentence boundaries update start and end counts")
    void sentenceBoundariesUpdateStartEndCountsAndBoundaryFlags() throws Exception {
        Path inputFile = writeInputFile("sentence-boundaries", "Alpha beta. Alpha gamma.");
        ImportService importService = new ImportService(new TextParser(new Tokenizer(), new Normalizer()));

        importService.importFile(inputFile, "transition-storage-" + System.nanoTime());

        assertEquals(
            2,
            queryForInt("SELECT start_count FROM words WHERE word_text = ?", "alpha"),
            "Expected alpha to be counted as the starting word for both sentences"
        );
        assertEquals(
            1,
            queryForInt("SELECT end_count FROM words WHERE word_text = ?", "beta"),
            "Expected beta to be counted as a sentence-ending word once"
        );
        assertEquals(
            1,
            queryForInt("SELECT end_count FROM words WHERE word_text = ?", "gamma"),
            "Expected gamma to be counted as a sentence-ending word once"
        );
        assertTrue(
            queryForBoolean(
                "SELECT follows_sentence_start FROM next_word nw " +
                    "JOIN words w1 ON w1.word_id = nw.from_word_id " +
                    "JOIN words w2 ON w2.word_id = nw.to_word_id " +
                    "WHERE w1.word_text = ? AND w2.word_text = ?",
                "alpha",
                "beta"
            ),
            "Expected alpha -> beta to be marked as following a sentence start"
        );
        assertTrue(
            queryForBoolean(
                "SELECT precedes_sentence_end FROM next_word nw " +
                    "JOIN words w1 ON w1.word_id = nw.from_word_id " +
                    "JOIN words w2 ON w2.word_id = nw.to_word_id " +
                    "WHERE w1.word_text = ? AND w2.word_text = ?",
                "alpha",
                "beta"
            ),
            "Expected alpha -> beta to be marked as preceding a sentence end"
        );
    }
}
// End of Code by Archisha Sasson
