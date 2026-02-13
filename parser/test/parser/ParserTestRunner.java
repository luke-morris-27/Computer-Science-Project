package parser;

import java.io.IOException;
import java.util.Map;
import java.nio.file.Path;

/*
 * Class: ParserTestRunner
 * Created by: Archisha Sasson
 * Description: Manual test runner that validates parser behavior against
 * simple and edge-case sample files with explicit expected values.
 */
// Code by Archisha Sasson
public class ParserTestRunner {
    private static final String SIMPLE_CASE = "simple";
    private static final String EDGE_CASE = "edge";

    public static void main(String[] args) {
        String testCase = args.length > 0 ? args[0].toLowerCase() : SIMPLE_CASE;

        try {
            switch (testCase) {
                case SIMPLE_CASE:
                    runSimpleCase();
                    break;
                case EDGE_CASE:
                    runEdgeCase();
                    break;
                default:
                    System.out.println("FAIL: Unknown test case '" + testCase + "'. Use 'simple' or 'edge'.");
                    System.exit(1);
            }
        } catch (IOException e) {
            System.out.println("FAIL: IOException while running tests: " + e.getMessage());
            System.exit(1);
        } catch (AssertionError e) {
            System.out.println("FAIL: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runSimpleCase() throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(Path.of("sample_texts/simple.txt"));

        assertEquals("simple totalWords", 8, result.getTotalWords());
        assertEquals("simple totalSentences", 3, result.getTotalSentences());
        assertCount("simple wordCounts[hello]", result.getWordCounts(), "hello", 2);
        assertCount("simple sentenceStartCounts[hello]", result.getSentenceStartCounts(), "hello", 2);
        assertCount("simple sentenceEndCounts[test]", result.getSentenceEndCounts(), "test", 1);
        assertNestedCount("simple nextWordCounts[hello][world]", result.getNextWordCounts(), "hello", "world", 1);

        System.out.println("PASS: simple test case");
    }

    private static void runEdgeCase() throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(Path.of("sample_texts/edge_cases.txt"));

        assertEquals("edge totalWords", 11, result.getTotalWords());
        assertEquals("edge totalSentences", 5, result.getTotalSentences());
        assertCount("edge wordCounts[don't]", result.getWordCounts(), "don't", 1);
        assertCount("edge wordCounts[mother-in-law]", result.getWordCounts(), "mother-in-law", 1);
        assertCount("edge sentenceStartCounts[well]", result.getSentenceStartCounts(), "well", 1);
        assertCount("edge sentenceStartCounts[maybe]", result.getSentenceStartCounts(), "maybe", 1);
        assertCount("edge sentenceEndCounts[believing]", result.getSentenceEndCounts(), "believing", 1);
        assertCount("edge sentenceEndCounts[well]", result.getSentenceEndCounts(), "well", 1);
        assertNestedCount("edge nextWordCounts[don't][stop]", result.getNextWordCounts(), "don't", "stop", 1);
        assertNestedCount("edge nextWordCounts[mother-in-law][is]", result.getNextWordCounts(), "mother-in-law", "is", 1);

        System.out.println("PASS: edge test case");
    }

    private static void assertEquals(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertCount(String label, Map<String, Integer> map, String key, int expected) {
        int actual = map.getOrDefault(key, 0);
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertNestedCount(
        String label,
        Map<String, Map<String, Integer>> map,
        String firstKey,
        String secondKey,
        int expected
    ) {
        int actual = map.getOrDefault(firstKey, Map.of()).getOrDefault(secondKey, 0);
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + ", actual=" + actual);
        }
    }
}
// End of Code by Archisha Sasson
