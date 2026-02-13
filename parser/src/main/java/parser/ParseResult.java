package parser;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Class: ParseResult
 * Created by: Archisha Sasson
 * Description: Stores aggregate parsing output, including word frequencies,
 * sentence start/end frequencies, next-word transitions, and file metadata.
 */
// Code by Archisha Sasson
public class ParseResult {
    private final Map<String, Integer> wordCounts = new LinkedHashMap<>();
    private final Map<String, Integer> sentenceStartCounts = new LinkedHashMap<>();
    private final Map<String, Integer> sentenceEndCounts = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> nextWordCounts = new LinkedHashMap<>();

    private String fileName;
    private int totalWords;
    private int totalSentences;
    private Instant importedAt;

    public Map<String, Integer> getWordCounts() {
        return wordCounts;
    }

    public Map<String, Integer> getSentenceStartCounts() {
        return sentenceStartCounts;
    }

    public Map<String, Integer> getSentenceEndCounts() {
        return sentenceEndCounts;
    }

    public Map<String, Map<String, Integer>> getNextWordCounts() {
        return nextWordCounts;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(int totalWords) {
        this.totalWords = totalWords;
    }

    public int getTotalSentences() {
        return totalSentences;
    }

    public void setTotalSentences(int totalSentences) {
        this.totalSentences = totalSentences;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }

    public void incrementWordCount(String word) {
        wordCounts.merge(word, 1, Integer::sum);
    }

    public void incrementSentenceStartCount(String word) {
        sentenceStartCounts.merge(word, 1, Integer::sum);
    }

    public void incrementSentenceEndCount(String word) {
        sentenceEndCounts.merge(word, 1, Integer::sum);
    }

    public void incrementNextWordCount(String currentWord, String nextWord) {
        nextWordCounts
            .computeIfAbsent(currentWord, ignored -> new LinkedHashMap<>())
            .merge(nextWord, 1, Integer::sum);
    }
}
// End of Code by Archisha Sasson
