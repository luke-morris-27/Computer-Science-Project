package parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/*
 * Class: Tokenizer
 * Created by: Archisha Sasson
 * Description: Converts raw text into tokens consisting of words and sentence
 * boundary markers.
 * Example: "Hi mom!" ---> ["Hi", "mom", "<SENTENCE_BOUNDARY>"]
 */

// Code by Archisha Sasson

public class Tokenizer {
    public static final String SENTENCE_BOUNDARY = "<SENTENCE_BOUNDARY>";
    // Defines the end of a sentence

    public List<String> tokenize(String rawText) {
        List<String> tokens = new ArrayList<>();
        if (rawText == null || rawText.isEmpty()) {
            return tokens;
        }

        StringBuilder currentWord = new StringBuilder();
        // Efficient way to build strings character by character

        boolean inBoundaryRun = false;
        // Will mark true if we see sentence-ending punctuation
        // This way we don't add multiple sentences for "!!!" or "?!?"

        // Looping through every character in the text
        for (int i = 0; i < rawText.length(); i++) {
            char ch = rawText.charAt(i);
            // Get the current character

            // Is ch part of a word (so letter, digit, apostrophe, or hyphen)
            if (isWordChar(ch)) {
                currentWord.append(ch); // Add ch to the word we are building
                inBoundaryRun = false; // Resetting boundary flag since we are in a word
                continue; // Skip rest of loop, because don't need to check if we are at the end
            }

            // Here, we hit a non-word character, so save currently built word
            flushWord(tokens, currentWord);

            // If it's a "." or "!" or "?"
            if (SentenceBoundary.isSentenceEndingChar(ch)) {
                if (!inBoundaryRun) {
                    tokens.add(SENTENCE_BOUNDARY);
                    inBoundaryRun = true; // So we don't count two consecutive boundary markers
                }
            } else if (!Character.isWhitespace(ch)) {
                // Handles other chars like commas, semicolons, etc.
                inBoundaryRun = false;
            }
        }

        // After loop, save current word and return list of token
        flushWord(tokens, currentWord);
        return tokens;
    }

    private boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '\'' || ch == '-';
    }

    // If no word built up, do nothing
    private void flushWord(List<String> tokens, StringBuilder currentWord) {
        if (currentWord.isEmpty()) { // If no word built up, do nothing
            return;
        }
        tokens.add(currentWord.toString()); // Add completed word to list
        currentWord.setLength(0); // Clear current counter
    }

    // Shriram Janardhan: Streaming overload - reads from Reader to avoid loading full file into memory
    public static class StreamResult {
        public final List<String> tokens;
        public final int paragraphCount;
        public StreamResult(List<String> tokens, int paragraphCount) {
            this.tokens = tokens;
            this.paragraphCount = paragraphCount;
        }
    }

    public StreamResult tokenizeStreaming(Reader reader) throws IOException {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();
        boolean inBoundaryRun = false;
        int paragraphCount = 1;
        boolean afterNewline = false;
        int ch;
        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            if (c == '\n') {
                if (afterNewline) paragraphCount++;
                afterNewline = true;
                flushWord(tokens, currentWord);
                inBoundaryRun = false;
                continue;
            }
            if (Character.isWhitespace(c)) {
                flushWord(tokens, currentWord);
                continue;
            }
            afterNewline = false;
            if (isWordChar(c)) {
                currentWord.append(c);
                inBoundaryRun = false;
                continue;
            }
            flushWord(tokens, currentWord);
            if (SentenceBoundary.isSentenceEndingChar(c)) {
                if (!inBoundaryRun) {
                    tokens.add(SENTENCE_BOUNDARY);
                    inBoundaryRun = true;
                }
            } else {
                inBoundaryRun = false;
            }
        }
        flushWord(tokens, currentWord);
        return new StreamResult(tokens, paragraphCount);
    }
}
// End of code by Shriram Janardhan (StreamResult, tokenizeStreaming)
// End of Code by Archisha Sasson

