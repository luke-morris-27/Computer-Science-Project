package generator;

import dao.NextWord;
import dao.WordQueryDao;

import java.sql.SQLException;
import java.util.List;

public class GreedyGenerator {

    private final WordQueryDao queryDao;

    public GreedyGenerator(WordQueryDao queryDao) {
        this.queryDao = queryDao;
    }

    /**
     * Deterministic greedy sentence generator.
     */
    public String generateGreedy(String startWord, int maxWords) throws SQLException {

        if (startWord == null || startWord.isBlank()) {
            return "Invalid start word.";
        }

        StringBuilder sentence = new StringBuilder();

        Integer currentId = queryDao.getWordId(startWord);
        if (currentId == null) {
            return "Start word not found.";
        }

        sentence.append(startWord);
        int wordCount = 1;

        // Simple safety guard to prevent infinite loops
        int safetyCounter = 0;
        int maxSafety = maxWords * 2;

        while (wordCount < maxWords) {

            safetyCounter++;
            if (safetyCounter > maxSafety) {
                break;
            }

            List<NextWord> nextWords = queryDao.getNextWords(currentId);

            if (nextWords.isEmpty()) {
                break;
            }

            // Always pick highest frequency (already sorted)
            NextWord next = nextWords.get(0);

            sentence.append(" ").append(next.wordText());

            currentId = next.wordId();
            wordCount++;
        }

        return sentence.toString();
    }
}
