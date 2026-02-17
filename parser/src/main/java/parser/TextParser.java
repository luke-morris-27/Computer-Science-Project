package parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/*
 * Class: TextParser
 * Created by: Archisha Sasson
 * Description: Orchestrates file parsing by reading text, tokenizing,
 * normalizing, and building ParseResult statistics.
 */
// Code by Archisha Sasson

public class TextParser {
    private final Tokenizer tokenizer;
    private final Normalizer normalizer;

    public TextParser() {
        this(new Tokenizer(), new Normalizer());
    }

    public TextParser(Tokenizer tokenizer, Normalizer normalizer) {
        this.tokenizer = tokenizer;
        this.normalizer = normalizer;
    }

    public ParseResult parse(Path file) throws IOException {
        // Sammy Pandey: Added input validation --------------------------------
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a regular file: " + file);
        }
        // ----------------------------------------------------------------------

        String rawText = Files.readString(file);
        List<String> tokens = tokenizer.tokenize(rawText); // Breaks text into tokens using tokenizer class

        ParseResult result = new ParseResult();
        result.setFileName(file.getFileName().toString());
        result.setImportedAt(Instant.now());

        // Sammy Pandey: For total paragraph count --------------------------------
        String[] paragraphs = rawText.split("\\n\\s*\\n");
        int paragraphCount = paragraphs.length;
        result.setTotalParagraphs(paragraphCount);
        // ------------------------------------------------------------------------

        String previousWord = null;
        String lastWordInSentence = null;
        boolean expectingSentenceStart = true;
        boolean sentenceHasWords = false;
        int totalWords = 0;
        int totalSentences = 0;

        // Sammy Pandey: Progress tracking -------------------
        int totalTokens = tokens.size();
        int processedTokens = 0;
        // ----------------------------------------------------

        // Going through each token
        for (String token : tokens) {
            // Sammy Pandey: Showing progress every 5000 tokens --------------
            // or could display it more often?
            processedTokens++;
            if (processedTokens % 5000 == 0) {
                System.out.println("Processed " + processedTokens + " / " + totalTokens + " tokens...");
            }
            // ! Might change this to a progress bar later
            // ----------------------------------------------------------------

            // CASE 1: Sentence Boundary
            if (SentenceBoundary.isSentenceBoundaryToken(token)) { // If we hit punctuation,
                if (sentenceHasWords && lastWordInSentence != null) { // and sentence actually has words,
                    result.incrementSentenceEndCount(lastWordInSentence); // then mark last word as a sentence ender,
                    totalSentences++; // and count this as a sentemce
                }
                // Restarting these for next sequence
                expectingSentenceStart = true;
                sentenceHasWords = false;
                lastWordInSentence = null;
                previousWord = null;
                continue;
            }

            // CASE 2: Regualar Word
            String word = normalizer.normalize(token);
            if (word.isEmpty()) { // If normalizer returns emtpy, it was all punctuation, so skip
                continue;
            }

            // Sammy Pandey: To track average word length --------------------------------
            result.addCharacters(word.length()); // Adding to the total chars
            // ---------------------------------------------------------------------------

            //Count this word
            result.incrementWordCount(word);
            totalWords++;

            if (expectingSentenceStart) {
                result.incrementSentenceStartCount(word);
                expectingSentenceStart = false;
            }

            if (previousWord != null) {
                result.incrementNextWordCount(previousWord, word);
            }

            previousWord = word;
            lastWordInSentence = word;
            sentenceHasWords = true;
        }

        if (sentenceHasWords && lastWordInSentence != null) {
            result.incrementSentenceEndCount(lastWordInSentence);
            totalSentences++;
        }

        result.setTotalWords(totalWords);
        result.setTotalSentences(totalSentences);
        return result;
    }
}
// End of Code by Archisha Sasson
