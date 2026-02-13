package parser;

import java.io.IOException;
import java.nio.file.Path;

/*
 * Class: TextParserTest
 * Created by: Archisha Sasson
 * Description: Basic parser smoke test that validates parse output on the
 * sample input file.
 */
// Code by Archisha Sasson
public class TextParserTest {
    public static void main(String[] args) throws IOException {
        TextParser parser = new TextParser();
        ParseResult result = parser.parse(Path.of("parser/sample_texts/simple.txt"));

        assert result.getTotalWords() > 0 : "Expected words to be parsed";
        assert result.getTotalSentences() > 0 : "Expected sentences to be parsed";
        assert !result.getWordCounts().isEmpty() : "Expected word counts to be populated";

        System.out.println("TextParserTest passed.");
    }
}
// End of Code by Archisha Sasson
