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
    public String normalize(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }

        if (SentenceBoundary.isSentenceBoundaryToken(token)) {
            return token;
        }

        int start = 0;
        int end = token.length() - 1;

        while (start <= end && isStripChar(token.charAt(start))) {
            start++;
        }
        while (end >= start && isStripChar(token.charAt(end))) {
            end--;
        }

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
