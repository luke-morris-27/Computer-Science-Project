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
        String rawText = Files.readString(file);
        List<String> tokens = tokenizer.tokenize(rawText);

        ParseResult result = new ParseResult();
        result.setFileName(file.getFileName().toString());
        result.setImportedAt(Instant.now());

        String previousWord = null;
        String lastWordInSentence = null;
        boolean expectingSentenceStart = true;
        boolean sentenceHasWords = false;
        int totalWords = 0;
        int totalSentences = 0;

        for (String token : tokens) {
            if (SentenceBoundary.isSentenceBoundaryToken(token)) {
                if (sentenceHasWords && lastWordInSentence != null) {
                    result.incrementSentenceEndCount(lastWordInSentence);
                    totalSentences++;
                }
                expectingSentenceStart = true;
                sentenceHasWords = false;
                lastWordInSentence = null;
                previousWord = null;
                continue;
            }

            String word = normalizer.normalize(token);
            if (word.isEmpty()) {
                continue;
            }

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
