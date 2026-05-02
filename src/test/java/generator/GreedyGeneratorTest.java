package generator;

/*
 * Class: GreedyGeneratorTest
 * Created by: Archisha Sasson 
 * Modified by: Omesh Sana
 * Description: Verifies greedy generator logic without a database by using
 * a fake repository with deterministic next-word ordering.
 * Example: The generator should always take the first weighted option.
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import parser.Normalizer;

@Tag("unit")
@Tag("generator")
@Tag("greedy-generator")
@DisplayName("Greedy Generator Unit Tests")
public class GreedyGeneratorTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Greedy generator picks the most common next word")
    void greedyGenerationChoosesHighestFrequencyAndStoresTheResult() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.wordIds.put("hello", 1);
        repository.nextWords.put(
            1,
            List.of(
                new WeightedWord(3, "there", 5),
                new WeightedWord(2, "world", 1)
            )
        );

        GreedyGenerator generator = new GreedyGenerator(repository, new Normalizer());

        String sentence = generator.generateGreedy("hello", 3);

        assertEquals("hello there", sentence, "Expected greedy generation to choose the first and highest-weight next word");
        assertEquals("hello there", repository.savedSentenceText, "Expected the greedy sentence to be stored");
        assertEquals("greedy", repository.savedAlgorithmName, "Expected the greedy algorithm label to be stored");
        assertEquals(1, repository.savedStartingWordId, "Expected the requested start word ID to be stored");
    }

    //Code by Archisha Sasson
    @Test
    @DisplayName("Greedy generator matches autocomplete alphabetical tie-break")
    void greedyGenerationUsesAutocompleteTieBreakForEqualFrequencyWords() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.wordIds.put("hello", 1);
        repository.nextWords.put(
            1,
            List.of(
                new WeightedWord(3, "zebra", 5),
                new WeightedWord(2, "apple", 5)
            )
        );

        GreedyGenerator generator = new GreedyGenerator(repository, new Normalizer());

        String sentence = generator.generateGreedy("hello", 3);

        assertEquals("hello apple", sentence, "Expected greedy generation to match autocomplete's alphabetical tie-break");
    }
    //End of Code by Archisha Sasson

    // Code by Shriram
    @Test
    @DisplayName("Greedy generator includes the user's start word even when it is not in the database")
    void greedyGenerationPrependsTheUserStartWordWhenItIsNotInTheDatabase() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.startWords = List.of(
            new WeightedWord(20, "beta", 4),
            new WeightedWord(10, "alpha", 1)
        );

        GreedyGenerator generator = new GreedyGenerator(repository, new Normalizer());

        String sentence = generator.generateGreedy("missing", 3);

        assertEquals("missing beta", sentence, "Expected the user's start word to appear first followed by the fallback word");
        assertEquals("missing beta", repository.savedSentenceText, "Expected the full sentence with prepended start word to be stored");
        assertEquals(20, repository.savedStartingWordId, "Expected the chosen fallback start word ID to be stored");
    }
    // End of Code by Shriram

    @Test
    @DisplayName("Greedy generator returns an empty result when no start word exists")
    void greedyGenerationReturnsAnEmptyStringWhenNoValidStartWordsExist() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        GreedyGenerator generator = new GreedyGenerator(repository, new Normalizer());

        String sentence = generator.generateGreedy(null, 5);

        assertEquals("", sentence, "Expected generation to return an empty string when there is no valid starting point");
        assertNull(repository.savedSentenceText, "Expected no sentence to be stored when generation could not start");
    }

    private static final class FakeGeneratorRepository implements GeneratorRepository {
        private final Map<String, Integer> wordIds = new HashMap<>();
        private final Map<Integer, List<WeightedWord>> nextWords = new HashMap<>();
        private List<WeightedWord> startWords = new ArrayList<>();

        private String savedSentenceText;
        private String savedAlgorithmName;
        private Integer savedStartingWordId;

        @Override
        public Integer getWordId(String wordText) {
            return wordIds.get(wordText);
        }

        @Override
        public List<WeightedWord> getNextWords(int wordId) {
            return nextWords.getOrDefault(wordId, List.of());
        }

        @Override
        public List<WeightedWord> getStartWords() {
            return startWords;
        }

        @Override
        public void saveGeneratedSentence(String sentenceText, String algorithmName, Integer startingWordId) {
            this.savedSentenceText = sentenceText;
            this.savedAlgorithmName = algorithmName;
            this.savedStartingWordId = startingWordId;
        }
    }
}
