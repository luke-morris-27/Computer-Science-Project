package integration;

/*
 * Class: WordStorageIT
 * Created by: Archisha Sasson
 * Description: Verifies ParserDB word-storage behavior including normalization,
 * unique word rows, and reuse of the same word_id for repeated occurrences.
 * Example: "Echo echo echo. Echo?" should produce one "echo" row in words.
 */

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import parser.Normalizer;
import parser.ParseResult;
import parser.TextParser;
import parser.Tokenizer;
import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Code by Archisha Sasson
@Tag("integration")
@Tag("parserdb")
@Tag("word-storage")
@DisplayName("Word Storage Integration Tests")
public class WordStorageIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Repeated words reuse the same database row")
    void repeatedOccurrencesReuseTheSameWordRowAndWordId() throws Exception {
        Path inputFile = writeInputFile("repeated-occurrences", "Echo echo echo. Echo?");
        TextParser parser = new TextParser(new Tokenizer(), new Normalizer(), true);

        ParseResult result = parser.parse(inputFile);

        assertEquals(4, result.getTotalWords(), "Expected all four repeated occurrences to be counted in memory");
        assertEquals(
            1,
            queryForInt("SELECT COUNT(*) FROM words WHERE word_text = ?", "echo"),
            "Expected repeated occurrences to create exactly one normalized row in words"
        );
        assertEquals(
            1,
            queryForInt("SELECT COUNT(DISTINCT word_id) FROM words WHERE word_text = ?", "echo"),
            "Expected the normalized word to map to a single reusable word_id"
        );
    }

    @Test
    @DisplayName("Stored words stay lowercase without duplicate mixed-case rows")
    void normalizationStoresLowercaseWordsWithoutDuplicateMixedCaseRows() throws Exception {
        Path inputFile = writeInputFile("normalized-words", "Hello, HELLO hello!");
        TextParser parser = new TextParser(new Tokenizer(), new Normalizer(), true);

        parser.parse(inputFile);

        assertEquals(
            1,
            queryForInt("SELECT COUNT(*) FROM words WHERE word_text = ?", "hello"),
            "Expected lowercase normalization to collapse all case variants into one row"
        );
        assertEquals(
            0,
            queryForInt("SELECT COUNT(*) FROM words WHERE word_text IN (?, ?)", "Hello", "HELLO"),
            "Expected unnormalized mixed-case rows to never be stored in words"
        );
    }
}
// End of Code by Archisha Sasson
