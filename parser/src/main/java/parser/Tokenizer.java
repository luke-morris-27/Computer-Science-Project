package parser;

import java.util.ArrayList;
import java.util.List;

/*
 * Class: Tokenizer
 * Created by: Archisha Sasson
 * Description: Converts raw text into tokens consisting of words and sentence
 * boundary markers.
 */
// Code by Archisha Sasson
public class Tokenizer {
    public static final String SENTENCE_BOUNDARY = "<SENTENCE_BOUNDARY>";

    public List<String> tokenize(String rawText) {
        List<String> tokens = new ArrayList<>();
        if (rawText == null || rawText.isEmpty()) {
            return tokens;
        }

        StringBuilder currentWord = new StringBuilder();
        boolean inBoundaryRun = false;

        for (int i = 0; i < rawText.length(); i++) {
            char ch = rawText.charAt(i);

            if (isWordChar(ch)) {
                currentWord.append(ch);
                inBoundaryRun = false;
                continue;
            }

            flushWord(tokens, currentWord);

            if (SentenceBoundary.isSentenceEndingChar(ch)) {
                if (!inBoundaryRun) {
                    tokens.add(SENTENCE_BOUNDARY);
                    inBoundaryRun = true;
                }
            } else if (!Character.isWhitespace(ch)) {
                inBoundaryRun = false;
            }
        }

        flushWord(tokens, currentWord);
        return tokens;
    }

    private boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'' || ch == '-';
    }

    private void flushWord(List<String> tokens, StringBuilder currentWord) {
        if (currentWord.length() == 0) {
            return;
        }
        tokens.add(currentWord.toString());
        currentWord.setLength(0);
    }
}
// End of Code by Archisha Sasson
