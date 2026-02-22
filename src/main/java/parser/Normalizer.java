package parser;

import java.util.Locale;

/*
 * Class: Normalizer
 * Created by: Archisha Sasson
 * Description: Normalizes tokens into a canonical word form by lowercasing
 * and trimming surrounding punctuation while preserving internal apostrophes
 * and hyphens.
 */
// Code by Archisha Sasson
public class Normalizer {

    // Taken a token/word and cleans it up to process
    public String normalize(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        // If it is a sentence boundary we don't want to change it so just return token
        if (SentenceBoundary.isSentenceBoundaryToken(token)) {
            return token;
        }

        int start = 0;
        int end = token.length() - 1;

        // Keep moving "start" forward to strip anything that is not letter/digit/apostrophe/hyphen
        while (start <= end && isStripChar(token.charAt(start))) {
            start++;
        }
        // If we hit a stip char, then move end backward to find the real end of the word
        while (end >= start && isStripChar(token.charAt(end))) {
            end--;
        }

        // If start passes end, then no word in there
        if (start > end) {
            return "";
        }

        return token.substring(start, end + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isStripChar(char ch) {
        return !Character.isLetterOrDigit(ch) && ch != '\'' && ch != '-';
    }
}
// End of Code by Archisha Sasson
