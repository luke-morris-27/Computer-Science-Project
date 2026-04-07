package generator;

/*
 * Class: RandomGeneratorTest
 * Created by: Omesh Sana
 * Description: Unit tests for RandomGenerator to verify it selects next words uniformly at random and correctly handles edge cases.
 * Example: A start word with no next words should result in an empty string, and a missing start word should trigger fallback logic.
 */


import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
@Tag("random-generator")
@DisplayName("Random Generator Unit Tests")
public class RandomGeneratorTest {

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName());
    }

    @Test
    @DisplayName("Random generator chooses one valid next word and stores the result")
    void randomGenerationChoosesAValidNextWordAndStoresTheResult() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.wordIds.put("hello", 1);
        repository.nextWords.put(
            1,
            List.of(
                new WeightedWord(2, "world", 1),
                new WeightedWord(3, "there", 5)
            )
        );

        RandomGenerator generator = new RandomGenerator(
            repository,
            new FixedRandom(1),
            new Normalizer()
        );

        String sentence = generator.generateRandom("hello", 3);

        assertEquals("hello there", sentence);
        assertEquals("hello there", repository.savedSentenceText);
        assertEquals("random", repository.savedAlgorithmName);
        assertEquals(1, repository.savedStartingWordId);
    }

    @Test
    @DisplayName("Random generator uses a fallback start word when needed")
    void randomGenerationFallsBackToRandomStartWordWhenStartWordIsMissing() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        repository.startWords = List.of(
            new WeightedWord(10, "alpha", 1),
            new WeightedWord(20, "beta", 4)
        );

        RandomGenerator generator = new RandomGenerator(
            repository,
            new FixedRandom(1),
            new Normalizer()
        );

        String sentence = generator.generateRandom("missing", 2);

        assertEquals("beta", sentence);
        assertEquals("beta", repository.savedSentenceText);
        assertEquals(20, repository.savedStartingWordId);
    }

    @Test
    @DisplayName("Random generator returns empty string when no start word exists")
    void randomGenerationReturnsEmptyStringWhenNoStartWordsExist() throws SQLException {
        FakeGeneratorRepository repository = new FakeGeneratorRepository();
        RandomGenerator generator = new RandomGenerator(
            repository,
            new FixedRandom(0),
            new Normalizer()
        );

        String sentence = generator.generateRandom(null, 5);

        assertEquals("", sentence);
        assertNull(repository.savedSentenceText);
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