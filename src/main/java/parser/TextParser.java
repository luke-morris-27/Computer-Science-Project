package parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import db.NextWordDao;
import db.WordCountsDao;

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

        boolean expectingSentenceStart = true;
        boolean sentenceHasWords = false;

        int totalWords = 0;
        int totalSentences = 0;

        // Shriram Janardhan: Progress bar - visual indicator for large files
        int totalTokens = tokens.size();
        int processedTokens = 0;

        // Sammy Pandey: DB state tracking (word IDs + sentence state) ----------------------------
        Integer prevWordId = null;
        Integer sentenceStartWordId = null;
        Integer lastWordIdInSentence = null;

        // Sammy Pandey: Track last transition so we can mark precedes_sentence_end at boundary time
        Integer lastFromId = null;
        Integer lastToId = null;
        // --------------------------------------------------------------------------------------

        // Shriram Janardhan: Database-backed unique word storage (MySQL) via WordDb.openConnection()
        // Sammy Pandey: DB wiring for next_word + start/end counts (uses Shriram's word_id lookups)
        try (Connection conn = WordDb.openConnection()) {
            // Sammy Pandey: Use a single transaction for this import
            conn.setAutoCommit(false);

            // Sammy Pandey: DAOs for start/end counts + transitions
            WordCountsDao countsDao = new WordCountsDao(conn);
            NextWordDao nextWordDao = new NextWordDao(conn);

            // Going through each token
            for (String token : tokens) {
                // Shriram Janardhan: Progress bar updates
                processedTokens++;
                if (processedTokens % 5000 == 0) {
                    printProgressBar(processedTokens, totalTokens);
                }

                // CASE 1: Sentence Boundary
                if (SentenceBoundary.isSentenceBoundaryToken(token)) { // Code by Archisha Sasson
                    if (sentenceHasWords && lastWordInSentence != null) { // Code by Archisha Sasson
                        result.incrementSentenceEndCount(lastWordInSentence); // Code by Archisha Sasson
                        totalSentences++; // Code by Archisha Sasson

                        // Sammy Pandey: DB end_count increments for last word in sentence
                        if (lastWordIdInSentence != null) {
                            countsDao.incEnd(lastWordIdInSentence);
                        }

                        // Sammy Pandey: mark last transition as precedes_sentence_end
                        if (lastFromId != null && lastToId != null) {
                            nextWordDao.markPrecedesEnd(lastFromId, lastToId);
                        }
                    }

                    // Reset for next sentence (Archisha Sasson base logic + Sammy ID resets)
                    expectingSentenceStart = true; // Code by Archisha Sasson
                    sentenceHasWords = false;      // Code by Archisha Sasson

                    previousWord = null; // Code by Archisha Sasson
                    prevWordId = null;   // Sammy Pandey

                    lastWordInSentence = null;   // Code by Archisha Sasson
                    lastWordIdInSentence = null; // Sammy Pandey

                    sentenceStartWordId = null; // Sammy Pandey
                    lastFromId = null;          // Sammy Pandey
                    lastToId = null;            // Sammy Pandey
                    continue;
                }

                // CASE 2: Regular Word
                String word = normalizer.normalize(token); // Code by Archisha Sasson (normalization)
                if (word.isEmpty()) { // Code by Archisha Sasson
                    continue;
                }

                // Shriram Janardhan: Ensure word exists in DB and get its ID (unique word storage)
                int wordId = WordDb.getOrCreateWordId(word, conn);

                // Sammy Pandey: Track average word length (ParseResult extension)
                result.addCharacters(word.length());

                // Count word (Archisha Sasson base logic)
                result.incrementWordCount(word); // Code by Archisha Sasson
                totalWords++;                    // Code by Archisha Sasson

                // Sentence start handling
                if (expectingSentenceStart) {
                    result.incrementSentenceStartCount(word); // Code by Archisha Sasson
                    expectingSentenceStart = false;          // Code by Archisha Sasson

                    // Sammy Pandey: DB start_count increments
                    countsDao.incStart(wordId);
                    sentenceStartWordId = wordId;
                }

                // In-memory transition map (Archisha Sasson base logic)
                if (previousWord != null) {
                    result.incrementNextWordCount(previousWord, word); // Code by Archisha Sasson
                }

                // Sammy Pandey: DB transition upsert + frequency increments
                if (prevWordId != null) {
                    boolean followsStart =
                        (sentenceStartWordId != null && prevWordId.equals(sentenceStartWordId));

                    // precedes_sentence_end is marked when we hit boundary (above)
                    nextWordDao.increment(prevWordId, wordId, followsStart, false);

                    // remember last transition inside current sentence
                    lastFromId = prevWordId;
                    lastToId = wordId;
                }

                // Update trackers (Archisha base + Sammy IDs)
                previousWord = word; // Code by Archisha Sasson
                prevWordId = wordId; // Sammy Pandey

                lastWordInSentence = word;       // Code by Archisha Sasson
                lastWordIdInSentence = wordId;   // Sammy Pandey

                sentenceHasWords = true; // Code by Archisha Sasson
            }

            // If file ends without a <SENTENCE_BOUNDARY>, count the last sentence too
            if (sentenceHasWords && lastWordInSentence != null) { // Code by Archisha Sasson
                result.incrementSentenceEndCount(lastWordInSentence); // Code by Archisha Sasson
                totalSentences++; // Code by Archisha Sasson

                // Sammy Pandey: DB end_count for final sentence
                if (lastWordIdInSentence != null) {
                    countsDao.incEnd(lastWordIdInSentence);
                }

                // Sammy Pandey: mark last transition of final sentence as precedes_sentence_end
                if (lastFromId != null && lastToId != null) {
                    nextWordDao.markPrecedesEnd(lastFromId, lastToId);
                }
            }

            // Sammy Pandey: Commit DB transaction
            conn.commit();
        } catch (SQLException e) {
            throw new IOException("DB error: " + e.getMessage(), e); // Sammy Pandey
        }

        result.setTotalWords(totalWords);
        result.setTotalSentences(totalSentences);
        return result;
    }

    // Shriram Janardhan: Renders progress bar e.g. [##########----------] 50%
    private static void printProgressBar(int current, int total) {
        final int width = 40;
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