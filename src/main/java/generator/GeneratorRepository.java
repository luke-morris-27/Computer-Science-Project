package generator;

/*
 * Class: GeneratorRepository
 * Created by: Archisha Sasson
 * Description: Defines the database-facing operations needed by generator
 * algorithms to look up words, transitions, sentence starts, and storage.
 * Example: getNextWords(5) returns weighted candidates after word_id 5.
 */

import java.sql.SQLException;
import java.util.List;

// Code by Archisha Sasson
public interface GeneratorRepository {
    Integer getWordId(String wordText) throws SQLException;

    List<WeightedWord> getNextWords(int wordId) throws SQLException;

    List<WeightedWord> getStartWords() throws SQLException;

    void saveGeneratedSentence(String sentenceText, String algorithmName, Integer startingWordId) throws SQLException;
}
// End of Code by Archisha Sasson
