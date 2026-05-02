package parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Class: TextParser
 * Created by: Archisha Sasson
 * Description: Orchestrates file parsing by reading text, tokenizing,
 * normalizing, and building ParseResult statistics.
 */
// Code by Archisha Sasson

public class TextParser {
    private final parser.Tokenizer tokenizer;
    private final Normalizer normalizer;

    public TextParser() {
        this(new parser.Tokenizer(), new Normalizer());
    }

    public TextParser(parser.Tokenizer tokenizer, Normalizer normalizer) {
        this.tokenizer = tokenizer;
        this.normalizer = normalizer;
    }

    /**
     * Backward-compatible constructor. The databaseWritesEnabled flag is ignored because this parser is pure
     * and never writes to the database (imports are persisted by {@link ImportService}).
     */
    public TextParser(parser.Tokenizer tokenizer, Normalizer normalizer, boolean databaseWritesEnabled) {
        this(tokenizer, normalizer);
    }

    /**
     * Parses a file into an import-ready payload. This method does not write to the database.
     */
    public ImportParseResult parseForImport(Path file) throws IOException {
        return parseInternal(file, true);
    }

    public ParseResult parse(Path file) throws IOException {
        return parseInternal(file, false).parseResult();
    }

    private ImportParseResult parseInternal(Path file, boolean includeTransitionStats) throws IOException {
        // Sammy Pandey: Added input validation --------------------------------
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a regular file: " + file);
        }
        // ----------------------------------------------------------------------

        // Shriram Janardhan: Streaming parser - uses BufferedReader to avoid loading full file into memory
        parser.Tokenizer.StreamResult streamResult;
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
        String sentenceStartWord = null;

        boolean expectingSentenceStart = true;
        boolean sentenceHasWords = false;

        int totalWords = 0;
        int totalSentences = 0;

        Map<TransitionKey, TransitionStats> transitions =
            includeTransitionStats ? new LinkedHashMap<>() : null;

        TransitionKey lastTransitionInSentence = null;

        // Progress bar - visual indicator for large files
        int totalTokens = tokens.size();
        int processedTokens = 0;
        for (String token : tokens) {
            processedTokens++;
            if (processedTokens % 5000 == 0) {
                printProgressBar(processedTokens, totalTokens);
            }

            if (SentenceBoundary.isSentenceBoundaryToken(token)) {
                if (sentenceHasWords && lastWordInSentence != null) {
                    result.incrementSentenceEndCount(lastWordInSentence);
                    totalSentences++;
                }

                if (transitions != null && lastTransitionInSentence != null) {
                    transitions
                        .computeIfAbsent(lastTransitionInSentence, ignored -> new TransitionStats())
                        .markPrecedesEnd();
                }

                expectingSentenceStart = true;
                sentenceHasWords = false;
                previousWord = null;
                lastWordInSentence = null;
                sentenceStartWord = null;
                lastTransitionInSentence = null;
                continue;
            }

            String word = normalizer.normalize(token);
            if (word.isEmpty()) {
                continue;
            }

            result.addCharacters(word.length());
            result.incrementWordCount(word);
            totalWords++;

            if (expectingSentenceStart) {
                result.incrementSentenceStartCount(word);
                expectingSentenceStart = false;
                sentenceStartWord = word;
            }

            if (previousWord != null) {
                result.incrementNextWordCount(previousWord, word);

                if (transitions != null) {
                    boolean followsStart = sentenceStartWord != null && previousWord.equals(sentenceStartWord);
                    TransitionKey key = new TransitionKey(previousWord, word);
                    transitions
                        .computeIfAbsent(key, ignored -> new TransitionStats())
                        .increment(followsStart);
                    lastTransitionInSentence = key;
                }
            }

            previousWord = word;
            lastWordInSentence = word;
            sentenceHasWords = true;
        }

        if (sentenceHasWords && lastWordInSentence != null) {
            result.incrementSentenceEndCount(lastWordInSentence);
            totalSentences++;
        }

        if (transitions != null && sentenceHasWords && lastTransitionInSentence != null) {
            transitions
                .computeIfAbsent(lastTransitionInSentence, ignored -> new TransitionStats())
                .markPrecedesEnd();
        }

        result.setTotalWords(totalWords);
        result.setTotalSentences(totalSentences);
        return new ImportParseResult(result, transitions == null ? Map.of() : transitions);
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
