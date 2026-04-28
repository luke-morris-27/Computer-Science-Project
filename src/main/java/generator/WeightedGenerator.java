package generator;

/*
 * Class: WeightedGenerator
 * Created by: Archisha Sasson
 * Description: Generates sentences by selecting next words using
 * frequency-weighted probability and stores results in the database.
 * Example: A word with frequency 6 is chosen about twice as often as one with 3.
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import db.DbGeneratorRepository;
import parser.Normalizer;

// Code by Archisha Sasson
public class WeightedGenerator {
    private static final String ALGORITHM_NAME = "weighted";

    private final GeneratorRepository repository;
    private final Random random;
    private final Normalizer normalizer;

    public WeightedGenerator() {
        this(new DbGeneratorRepository(), new Random(), new Normalizer());
    }

    public WeightedGenerator(Random random) {
        this(new DbGeneratorRepository(), random, new Normalizer());
    }

    public WeightedGenerator(GeneratorRepository repository, Random random, Normalizer normalizer) {
        this.repository = repository;
        this.random = random;
        this.normalizer = normalizer;
    }

    public String generateWeighted(String startWord, int maxWords) throws SQLException {
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

            WeightedWord chosen = chooseWeighted(nextWords);
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
        return chooseWeighted(startWords);
    }

    //Code by Archisha Sasson
    public WeightedWord chooseWeightedSuggestion(List<WeightedWord> options) {
        return chooseWeighted(options);
    }
    //End of Code by Archisha Sasson

    // Code by Shriram
    // picks unique weighted suggestions for autocomplete using weighted random selection without replacement
    public List<WeightedWord> pickWeightedSuggestions(List<WeightedWord> candidates, int limit) {
        if (limit <= 0 || candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // copies the candidate list so we can remove picked words without changing the input
        List<WeightedWord> remaining = new ArrayList<>(candidates);
        List<WeightedWord> chosen = new ArrayList<>();

        // keeps picking weighted-random words until we hit the limit or run out
        while (chosen.size() < limit && !remaining.isEmpty()) {
            WeightedWord pick = chooseWeighted(remaining);
            if (pick == null) {
                break;
            }
            chosen.add(pick);
            remaining.remove(pick);
        }

        return chosen;
    }
    // End of Code by Shriram

    private WeightedWord chooseWeighted(List<WeightedWord> options) {
        int totalWeight = 0;
        for (WeightedWord option : options) {
            if (option.weight() > 0) {
                totalWeight += option.weight();
            }
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int runningTotal = 0;
        for (WeightedWord option : options) {
            if (option.weight() <= 0) {
                continue;
            }
            runningTotal += option.weight();
            if (roll < runningTotal) {
                return option;
            }
        }

        return options.get(options.size() - 1);
    }
}
// End of Code by Archisha Sasson
