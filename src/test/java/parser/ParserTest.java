package parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/*
 * Class: ParserTest
 * Created by: Archisha Sasson
 * Description: JUnit 5 test suite that validates parser behavior against
 * sample files with explicit expected values.
 */
// Code by Archisha Sasson
public class ParserTest {
    @Test
    void simpleCase() throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(resourcePath("simple.txt"));

        assertEquals(8, result.getTotalWords(), "simple totalWords");
        assertEquals(3, result.getTotalSentences(), "simple totalSentences");
        // Shriram Janardhan: Assert paragraph count
        assertEquals(1, result.getTotalParagraphs(), "simple totalParagraphs");
        assertCount("simple wordCounts[hello]", result.getWordCounts(), "hello", 2);
        assertCount("simple sentenceStartCounts[hello]", result.getSentenceStartCounts(), "hello", 2);
        assertCount("simple sentenceEndCounts[test]", result.getSentenceEndCounts(), "test", 1);
        assertNestedCount("simple nextWordCounts[hello][world]", result.getNextWordCounts(), "hello", "world", 1);
    }

    @Test
    void edgeCase() throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(resourcePath("edge_cases.txt"));

        assertEquals(11, result.getTotalWords(), "edge totalWords");
        assertEquals(5, result.getTotalSentences(), "edge totalSentences");
        // Shriram Janardhan: Assert paragraph count
        assertEquals(1, result.getTotalParagraphs(), "edge totalParagraphs");
        assertCount("edge wordCounts[don't]", result.getWordCounts(), "don't", 1);
        assertCount("edge wordCounts[mother-in-law]", result.getWordCounts(), "mother-in-law", 1);
        assertCount("edge sentenceStartCounts[well]", result.getSentenceStartCounts(), "well", 1);
        assertCount("edge sentenceStartCounts[maybe]", result.getSentenceStartCounts(), "maybe", 1);
        assertCount("edge sentenceEndCounts[believing]", result.getSentenceEndCounts(), "believing", 1);
        assertCount("edge sentenceEndCounts[well]", result.getSentenceEndCounts(), "well", 1);
        assertNestedCount("edge nextWordCounts[don't][stop]", result.getNextWordCounts(), "don't", "stop", 1);
        assertNestedCount("edge nextWordCounts[mother-in-law][is]", result.getNextWordCounts(), "mother-in-law", "is", 1);
    }

    // Shriram Janardhan: Tests paragraph counting (streaming parser)
    @Test
    void paragraphCase() throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(resourcePath("paragraphs.txt"));
        assertEquals(2, result.getTotalParagraphs(), "paragraph totalParagraphs");
        assertEquals(9, result.getTotalWords(), "paragraph totalWords");
        assertEquals(4, result.getTotalSentences(), "paragraph totalSentences");
    }
    // End of code by Shriram Janardhan (paragraph test, assertions)

    private static Path resourcePath(String fileName) {
        URL resource = ParserTest.class.getClassLoader().getResource("parser/" + fileName);
        assertNotNull(resource, "Missing test resource: " + fileName);
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid resource URI for test file: " + fileName, e);
        }
    }

    private static void assertCount(String label, Map<String, Integer> map, String key, int expected) {
        int actual = map.getOrDefault(key, 0);
        assertEquals(expected, actual, label);
    }

    private static void assertNestedCount(
        String label,
        Map<String, Map<String, Integer>> map,
        String firstKey,
        String secondKey,
        int expected
    ) {
        int actual = map.getOrDefault(firstKey, Map.of()).getOrDefault(secondKey, 0);
        assertEquals(expected, actual, label);
    }
}
// End of Code by Archisha Sasson
