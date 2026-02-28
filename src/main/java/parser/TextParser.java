package parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import db.Db;
import db.NextWordDao;
import db.WordCountsDao;
import db.WordDao;

import java.sql.Connection;
import java.sql.SQLException;

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

        // Shriram Janardhan: Streaming parser - uses BufferedReader to avoid loading full file into memory
        Tokenizer.StreamResult streamResult;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            streamResult = tokenizer.tokenizeStreaming(reader);
        }
        List<String> tokens = streamResult.tokens;
        // -----------------------------------------------------------------------

        ParseResult result = new ParseResult();
        result.setFileName(file.getFileName().toString());
        result.setImportedAt(Instant.now());
        // Shriram Janardhan: Paragraph count from streaming tokenizer
        result.setTotalParagraphs(streamResult.paragraphCount);

        String previousWord = null;
        String lastWordInSentence = null;
        Integer lastWordIdInSentence = null; // Sammy Pandey 2/27
        boolean expectingSentenceStart = true;
        boolean sentenceHasWords = false;
        int totalWords = 0;
        int totalSentences = 0;

        // Sammy Pandey: Progress tracking -------------------
        int totalTokens = tokens.size();
        int processedTokens = 0;
        // ----------------------------------------------------

        // Sammy Pandey 2/27: DB wiring for next_word + start/end counts --------------------------
        try (Connection conn = Db.connect()) { // Sammy Pandey 2/27
            conn.setAutoCommit(false); // Sammy Pandey 2/27

            WordDao wordDao = new WordDao(conn); // Sammy Pandey 2/27
            WordCountsDao countsDao = new WordCountsDao(conn); // Sammy Pandey 2/27
            NextWordDao nextWordDao = new NextWordDao(conn); // Sammy Pandey 2/27

            Integer prevWordId = null; // Sammy Pandey 2/27
            Integer sentenceStartWordId = null; // Sammy Pandey 2/27

            // Sammy Pandey 2/27: track last transition so we can mark precedes_sentence_end
            Integer lastFromId = null; // Sammy Pandey 2/27
            Integer lastToId = null;   // Sammy Pandey 2/27

            // Going through each token
            for (String token : tokens) {
                // Shriram Janardhan: Progress bar - visual indicator for large files
                processedTokens++;
                if (processedTokens % 5000 == 0) {
                    printProgressBar(processedTokens, totalTokens);
                }
                // ----------------------------------------------------------------

                // CASE 1: Sentence Boundary
                if (SentenceBoundary.isSentenceBoundaryToken(token)) { // If we hit punctuation,
                    if (sentenceHasWords && lastWordInSentence != null) { // and sentence actually has words,
                        result.incrementSentenceEndCount(lastWordInSentence); // then mark last word as a sentence ender,
                        if (lastWordIdInSentence != null) { // Sammy Pandey 2/27
                            countsDao.incEnd(lastWordIdInSentence); // Sammy Pandey 2/27
                        }

                        // Sammy Pandey 2/27: mark last transition in this sentence as precedes_sentence_end
                        if (lastFromId != null && lastToId != null) { // Sammy Pandey 2/27
                            nextWordDao.markPrecedesEnd(lastFromId, lastToId); // Sammy Pandey 2/27
                        }

                        totalSentences++; // and count this as a sentemce
                    }
                    // Restarting these for next sequence
                    expectingSentenceStart = true;
                    sentenceHasWords = false;
                    lastWordInSentence = null;
                    lastWordIdInSentence = null; // Sammy Pandey 2/27
                    previousWord = null;

                    prevWordId = null; // Sammy Pandey 2/27
                    sentenceStartWordId = null; // Sammy Pandey 2/27

                    lastFromId = null; // Sammy Pandey 2/27
                    lastToId = null;   // Sammy Pandey 2/27
                    continue;
                }

                // CASE 2: Regualar Word
                String word = normalizer.normalize(token);
                if (word.isEmpty()) { // If normalizer returns emtpy, it was all punctuation, so skip
                    continue;
                }

                int wordId = wordDao.getOrCreateWordId(word); // Sammy Pandey 2/27

                // Sammy Pandey: To track average word length --------------------------------
                result.addCharacters(word.length()); // Adding to the total chars
                // ---------------------------------------------------------------------------

                // Count this word
                result.incrementWordCount(word);
                totalWords++;

                if (expectingSentenceStart) {
                    result.incrementSentenceStartCount(word);
                    expectingSentenceStart = false;

                    countsDao.incStart(wordId); // Sammy Pandey 2/27
                    sentenceStartWordId = wordId; // Sammy Pandey 2/27
                }

                if (previousWord != null) {
                    result.incrementNextWordCount(previousWord, word);
                }

                // Sammy Pandey 2/27: DB transition upsert (frequency increments correctly)
                if (prevWordId != null) { // Sammy Pandey 2/27
                    boolean followsStart =
                            (sentenceStartWordId != null && prevWordId.equals(sentenceStartWordId)); // Sammy Pandey 2/27

                    // NOTE: precedes_sentence_end is marked at boundary time (see above)
                    nextWordDao.increment(prevWordId, wordId, followsStart, false); // Sammy Pandey 2/27

                    // Remember last transition inside the current sentence
                    lastFromId = prevWordId; // Sammy Pandey 2/27
                    lastToId = wordId;       // Sammy Pandey 2/27
                }
                // -------------------------------------------------------

                previousWord = word;
                prevWordId = wordId; // Sammy Pandey 2/27
                lastWordInSentence = word;
                lastWordIdInSentence = wordId; // Sammy Pandey 2/27
                sentenceHasWords = true;
            }

            // If file ends without a <SENTENCE_BOUNDARY>, count the last sentence too
            if (sentenceHasWords && lastWordInSentence != null) {
                result.incrementSentenceEndCount(lastWordInSentence);

                if (lastWordIdInSentence != null) { // Sammy Pandey 2/27
                    countsDao.incEnd(lastWordIdInSentence); // Sammy Pandey 2/27
                }

                // Sammy Pandey 2/27: mark last transition of final sentence as precedes_sentence_end
                if (lastFromId != null && lastToId != null) { // Sammy Pandey 2/27
                    nextWordDao.markPrecedesEnd(lastFromId, lastToId); // Sammy Pandey 2/27
                }

                totalSentences++;
            }

            conn.commit(); // Sammy Pandey 2/27
        } catch (SQLException e) {
            throw new IOException("DB error: " + e.getMessage(), e); // Sammy Pandey 2/27
        }
        // ----------------------------------------------------------------------------------------

        result.setTotalWords(totalWords);
        result.setTotalSentences(totalSentences);
        return result;
    }

    // Shriram Janardhan: Renders progress bar e.g. [##########----------] 50%
    private static void printProgressBar(int current, int total) {
        final int width = 40;
        // Use long arithmetic to avoid integer overflow for very large files
        long cur = current;
        long tot = total;

        int pct = 0;
        int filled = 0;
        if (tot > 0) {
            pct = (int) Math.min(100, Math.max(0, (cur * 100L) / tot));
            filled = (int) Math.min(width, Math.max(0, (cur * width) / tot));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\r[");
        for (int i = 0; i < width; i++) sb.append(i < filled ? '#' : '-');
        sb.append("] ").append(pct).append("%");
        System.out.print(sb);
        if (current >= total) System.out.println();
    }
}
// End of code by Shriram Janardhan (streaming, progress bar)
// End of Code by Archisha Sasson