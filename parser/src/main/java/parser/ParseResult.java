package parser;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Class: ParseResult
 * Created by: Archisha Sasson
 * Description: Data Holder; Stores aggregate parsing output, including word frequencies,
 * sentence start/end frequencies, next-word transitions, and file metadata.
 */
// Code by Archisha Sasson
public class ParseResult {
    // Maps the words to their counts:
    private final Map<String, Integer> wordCounts = new LinkedHashMap<>();
    // If a word appears here, it CAN start a sentence. If not, it has not started sentence:
    private final Map<String, Integer> sentenceStartCounts = new LinkedHashMap<>();
    private final Map<String, Integer> sentenceEndCounts = new LinkedHashMap<>();
    // A nested map, so for each word, what words follow it and how often (important for generating)
    private final Map<String, Map<String, Integer>> nextWordCounts = new LinkedHashMap<>();

    // Sammy Pandey: To track average word length -----------------------------
    // (Maybe be useful for database/reports?)
    private int totalCharacters = 0;

    public void addCharacters(int count) {
        totalCharacters += count;
    }

    /* Not using yet, but later might add to Main like this:
        System.out.println("Avg word length: " +
        String.format("%.2f", result.getAverageWordLength()) + " chars");
     */
    public double getAverageWordLength() {
        if (totalWords == 0) return 0;
        return (double) totalCharacters / totalWords;
    }
    // --------------------------------------------------------------------------

    private String fileName;
    private int totalWords;
    private int totalSentences;
    private Instant importedAt;
    private int totalParagraphs; // Sammy Pandey: for paragraph counter

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

    // Sammy Pandey: for total paragraph counter -------------------------
    public int getTotalParagraphs() {
        return totalParagraphs;
    }

    public void setTotalParagraphs(int totalParagraphs) {
        this.totalParagraphs = totalParagraphs;
    }
    // --------------------------------------------------------------------

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
