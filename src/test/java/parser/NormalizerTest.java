package parser;

/*
 * Class: NormalizerTest
 * Created by: Archisha Sasson
 * Description: Verifies normalization behavior for lowercase conversion,
 * punctuation trimming, and preservation of internal apostrophes/hyphens.
 * Example: "\"Don't\"" ---> "don't"
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Code by Archisha Sasson
@Tag("unit")
@Tag("parserdb")
@DisplayName("ParserDB Normalizer Unit Tests")
public class NormalizerTest {
    private final Normalizer normalizer = new Normalizer();

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Lowercases repeated word variants into one canonical form")
    void lowercasesRepeatedWordVariants() {
        assertEquals("hello", normalizer.normalize("Hello"), "Expected mixed-case token to normalize to lowercase");
        assertEquals("hello", normalizer.normalize("HELLO"), "Expected uppercase token to normalize to lowercase");
        assertEquals("hello", normalizer.normalize("hello"), "Expected lowercase token to remain lowercase");
    }

    @Test
    @DisplayName("Preserves internal apostrophes and hyphens while trimming punctuation")
    void preservesInternalApostrophesAndHyphens() {
        assertEquals("don't", normalizer.normalize("\"Don't\""), "Expected apostrophes to remain after punctuation trimming");
        assertEquals("mother-in-law", normalizer.normalize("(Mother-in-law)"), "Expected hyphenated words to remain intact");
    }

    @Test
    @DisplayName("Returns an empty string for punctuation-only tokens")
    void stripsPunctuationOnlyTokens() {
        assertEquals("", normalizer.normalize("..."), "Expected punctuation-only tokens to be dropped");
    }
}
// End of Code by Archisha Sasson
