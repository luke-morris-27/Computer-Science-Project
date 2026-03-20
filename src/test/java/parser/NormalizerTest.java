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
@DisplayName("Normalizer Unit Tests")
public class NormalizerTest {
    private final Normalizer normalizer = new Normalizer();

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Normalizer turns different letter cases into one lowercase word")
    void lowercasesRepeatedWordVariants() {
        assertEquals("hello", normalizer.normalize("Hello"), "Expected mixed-case token to normalize to lowercase");
        assertEquals("hello", normalizer.normalize("HELLO"), "Expected uppercase token to normalize to lowercase");
        assertEquals("hello", normalizer.normalize("hello"), "Expected lowercase token to remain lowercase");
    }

    @Test
    @DisplayName("Normalizer keeps apostrophes and hyphens inside words")
    void preservesInternalApostrophesAndHyphens() {
        assertEquals("don't", normalizer.normalize("\"Don't\""), "Expected apostrophes to remain after punctuation trimming");
        assertEquals("mother-in-law", normalizer.normalize("(Mother-in-law)"), "Expected hyphenated words to remain intact");
    }

    @Test
    @DisplayName("Normalizer removes tokens that are only punctuation")
    void stripsPunctuationOnlyTokens() {
        assertEquals("", normalizer.normalize("..."), "Expected punctuation-only tokens to be dropped");
    }
}
// End of Code by Archisha Sasson
