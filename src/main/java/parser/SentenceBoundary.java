package parser;

/*
 * Class: SentenceBoundary
 * Created by: Archisha Sasson
 * Description: Utility class for sentence boundary detection using punctuation
 * and tokenizer boundary tokens.
 */
// Code by Archisha Sasson
public final class SentenceBoundary {
    private SentenceBoundary() {
    }

    public static boolean isSentenceEndingChar(char ch) {
        return ch == '.' || ch == '!' || ch == '?';
    }

    public static boolean isSentenceBoundaryToken(String token) {
        return Tokenizer.SENTENCE_BOUNDARY.equals(token);
    }
}
// End of Code by Archisha Sasson
