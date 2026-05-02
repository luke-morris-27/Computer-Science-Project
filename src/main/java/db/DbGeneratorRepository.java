package db;

/*
 * Class: DbGeneratorRepository
 * Created by: Archisha Sasson
 * Description: Implements generator database lookups and generated sentence
 * storage using the application's relational schema.
 * Example: Reads next-word candidates from next_word and words tables.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import generator.GeneratorRepository;
import generator.WeightedWord;
import parser.WordDb;

// Code by Archisha Sasson
public class DbGeneratorRepository implements GeneratorRepository {
    @Override
    public Integer getWordId(String wordText) throws SQLException {
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT word_id FROM words WHERE word_text = ?"
             )) {
            ps.setString(1, wordText);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("word_id");
                }
                return null;
            }
        }
    }

    @Override
    public List<WeightedWord> getNextWords(int wordId) throws SQLException {
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT w.word_id, w.word_text, nw.transition_count " +
                     "FROM next_word nw " +
                     "JOIN words w ON w.word_id = nw.to_word_id " +
                     "WHERE nw.from_word_id = ? " +
                     //Code by Archisha Sasson
                     "ORDER BY nw.transition_count DESC, w.word_text ASC"
                     //End of Code by Archisha Sasson
             )) {
            ps.setInt(1, wordId);
            try (ResultSet rs = ps.executeQuery()) {
                return readWeightedWords(rs, "transition_count");
            }
        }
    }

    @Override
    public List<WeightedWord> getStartWords() throws SQLException {
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT word_id, word_text, start_count " +
                     "FROM words " +
                     "WHERE start_count > 0 " +
                     "ORDER BY start_count DESC, word_id ASC"
             );
             ResultSet rs = ps.executeQuery()) {
            return readWeightedWords(rs, "start_count");
        }
    }

    @Override
    public void saveGeneratedSentence(String sentenceText, String algorithmName, Integer startingWordId) throws SQLException {
        if (sentenceText == null || sentenceText.isBlank()) {
            return;
        }

        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO generated_sentences (sentence_text, algorithm_name, starting_word_id) " +
                     "VALUES (?, ?, ?)"
             )) {
            ps.setString(1, sentenceText);
            ps.setString(2, algorithmName);
            if (startingWordId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, startingWordId);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!generatedSentenceExists(sentenceText)) {
                throw e;
            }
        }
    }

    private boolean generatedSentenceExists(String sentenceText) throws SQLException {
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT sentence_id FROM generated_sentences WHERE sentence_text = ?"
             )) {
            ps.setString(1, sentenceText);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<WeightedWord> readWeightedWords(ResultSet rs, String weightColumn) throws SQLException {
        List<WeightedWord> words = new ArrayList<>();
        while (rs.next()) {
            words.add(new WeightedWord(
                rs.getInt("word_id"),
                rs.getString("word_text"),
                rs.getInt(weightColumn)
            ));
        }
        return words;
    }
}
// End of Code by Archisha Sasson
