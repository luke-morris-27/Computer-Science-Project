package generator;

/*
 * Class: WeightedGeneratorTest
 * Created by: Archisha Sasson
 * Description: Verifies weighted generator logic without a database by using
 * a fake repository and deterministic random values.
 * Example: A higher-frequency next word should be selected when the roll lands in its range.
 */

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import parser.Normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Code by Archisha Sasson
@Tag("unit")
@Tag("generator")
@Tag("weighted-generator")
@DisplayName("Generator Weighted Unit Tests")
public class WeightedGeneratorTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Weighted generation prefers higher-frequency next words based on the random roll")
    void weightedGenerationPrefersHigherFrequencyNextWords() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.wordIds.put("hello", 1);
        repository.nextWords.put(
            1,
            List.of(
                new WeightedWord(2, "world", 1),
                new WeightedWord(3, "there", 3)
            )
        );

        WeightedGenerator generator = new WeightedGenerator(
            repository,
            new FixedRandom(2),
            new Normalizer()
        );

        String sentence = generator.generateWeighted("hello", 3);

        assertEquals("hello there", sentence, "Expected the weighted roll to choose the higher-frequency next word");
        assertEquals("hello there", repository.savedSentenceText, "Expected the generated sentence to be stored");
        assertEquals("weighted", repository.savedAlgorithmName, "Expected the weighted algorithm name to be stored");
        assertEquals(1, repository.savedStartingWordId, "Expected the original start word ID to be stored");
    }

    @Test
    @DisplayName("Weighted generation falls back to start words when the requested start word is missing")
    void weightedGenerationFallsBackToStartWordsWhenStartWordIsMissing() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.startWords = List.of(
            new WeightedWord(10, "alpha", 1),
            new WeightedWord(20, "beta", 4)
        );

        WeightedGenerator generator = new WeightedGenerator(
            repository,
            new FixedRandom(3),
            new Normalizer()
        );

        String sentence = generator.generateWeighted("missing", 2);

        assertEquals("beta", sentence, "Expected fallback start-word selection to use the weighted start words");
        assertEquals("beta", repository.savedSentenceText, "Expected fallback generation to still be stored");
        assertEquals(20, repository.savedStartingWordId, "Expected the chosen fallback start word ID to be stored");
    }

    @Test
    @DisplayName("Weighted generation returns an empty string when no start words exist")
    void weightedGenerationReturnsEmptyStringWhenNoStartWordsExist() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        WeightedGenerator generator = new WeightedGenerator(
            repository,
            new FixedRandom(0),
            new Normalizer()
        );

        String sentence = generator.generateWeighted(null, 5);

        assertEquals("", sentence, "Expected generation to return an empty string when no start words are available");
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

    private static final class FixedRandom extends Random {
        private final Deque<Integer> values = new ArrayDeque<>();

        private FixedRandom(int... values) {
            for (int value : values) {
                this.values.addLast(value);
            }
        }

        @Override
        public int nextInt(int bound) {
            if (values.isEmpty()) {
                return 0;
            }
            int next = values.removeFirst();
            return Math.floorMod(next, bound);
        }
    }
}
// End of Code by Archisha Sasson
