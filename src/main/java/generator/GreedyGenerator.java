package generator;

/*
 * Class: GreedyGenerator
 * Created by: Luke Morris 
 * Description: Generates sentences by always choosing the highest-frequency
 * next word from the repository and stores the generated result.
 * Example: If "there" has a higher transition count than "world", greedy
 * generation always picks "there" next.
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import db.DbGeneratorRepository;
import parser.Normalizer;

public class GreedyGenerator {
    private static final String ALGORITHM_NAME = "greedy";

    private final GeneratorRepository repository;
    private final Normalizer normalizer;

    public GreedyGenerator() {
        this(new DbGeneratorRepository(), new Normalizer());
    }

    public GreedyGenerator(GeneratorRepository repository, Normalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    public String generateGreedy(String startWord, int maxWords) throws SQLException {
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
            WeightedWord chosen = chooseGreedy(nextWords);
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

        return chooseGreedy(repository.getStartWords());
    }

    //Code by Archisha Sasson
    private WeightedWord chooseGreedy(List<WeightedWord> options) {
        if (options == null) {
            return null;
        }

        return options.stream()
            .filter(option -> option != null && option.weight() > 0)
            .sorted(java.util.Comparator
                .comparingInt(WeightedWord::weight)
                .reversed()
                .thenComparing(WeightedWord::wordText))
            .findFirst()
            .orElse(null);
    }
    //End of Code by Archisha Sasson
}
