package generator;

/*
 * Class: WeightedGenerator
 * Created by: Omesh sana
 * Description: Generates sentences by selecting next words uniformly at random and stores results in the database.
 * Example: A word with frequency 6 is chosen just as often as one with 3.
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import db.DbGeneratorRepository;
import parser.Normalizer;

public class RandomGenerator {
    private static final String ALGORITHM_NAME = "random";

    private final GeneratorRepository repository;
    private final Random random;
    private final Normalizer normalizer;

    public RandomGenerator() {
        this(new DbGeneratorRepository(), new Random(), new Normalizer());
    }

    public RandomGenerator(GeneratorRepository repository, Random random, Normalizer normalizer) {
        this.repository = repository;
        this.random = random;
        this.normalizer = normalizer;
    }

    public String generateRandom(String startWord, int maxWords) throws SQLException {
        if (maxWords <= 0) {
            return "";
        }

        // Code by Shriram
        // checks if the user provided a start word that is not in the database
        String normalizedStart = normalizer.normalize(startWord);
        boolean userProvidedWord = !normalizedStart.isEmpty();
        // End of Code by Shriram

        WeightedWord start = resolveStartWord(startWord);
        if (start == null) {
            // Code by Shriram
            // returns just the user's word when no database words exist
            if (userProvidedWord) {
                repository.saveGeneratedSentence(normalizedStart, ALGORITHM_NAME, null);
                return normalizedStart;
            }
            // End of Code by Shriram
            return "";
        }

        List<String> generatedWords = new ArrayList<>();
        // Code by Shriram
        // prepends the user's start word if it was not found in the database
        if (userProvidedWord && !normalizedStart.equals(start.wordText())) {
            generatedWords.add(normalizedStart);
        }
        // End of Code by Shriram
        generatedWords.add(start.wordText());

        int startingWordId = start.wordId();
        int currentWordId = start.wordId();

        while (generatedWords.size() < maxWords) {
            List<WeightedWord> nextWords = repository.getNextWords(currentWordId);
            if (nextWords.isEmpty()) {
                break;
            }

            WeightedWord chosen = chooseRandom(nextWords);
            if (chosen == null) {
                break;
            }

            generatedWords.add(chosen.wordText());
            currentWordId = chosen.wordId();
        }

        String sentence = String.join(" ", generatedWords);
        repository.saveGeneratedSentence(sentence, ALGORITHM_NAME, startingWordId);
        return sentence;
    }

    private WeightedWord resolveStartWord(String startWord) throws SQLException {
        String normalizedStart = normalizer.normalize(startWord);
        if (!normalizedStart.isEmpty()) {
            Integer wordId = repository.getWordId(normalizedStart);
            if (wordId != null) {
                return new WeightedWord(wordId, normalizedStart, 1);
            }
        }

        List<WeightedWord> startWords = repository.getStartWords();
        if (startWords.isEmpty()) {
            return null;
        }
        return chooseRandom(startWords);
    }

    private WeightedWord chooseRandom(List<WeightedWord> options) {
        List<WeightedWord> valid = new ArrayList<>();
        for (WeightedWord option : options) {
            if (option.weight() > 0) {
                valid.add(option);
            }
        }
        if (valid.isEmpty()) {
            return null;
        }
        return valid.get(random.nextInt(valid.size()));
    }
}