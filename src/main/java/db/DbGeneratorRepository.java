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
        // sammy 5/2: skip storage when generation produced no usable sentence text.
        if (sentenceText == null || sentenceText.isBlank()) {
            // sammy 5/2: returning here keeps the table free of blank history rows.
            return;
        }

        // sammy 5/2: open one connection for the sentence-history insert.
        try (Connection conn = WordDb.openConnection();
             // sammy 5/2: insert every generated sentence row so duplicate reporting can work.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO generated_sentences (sentence_text, algorithm_name, starting_word_id) " +
                     "VALUES (?, ?, ?)"
             )) {
            // sammy 5/2: store the exact sentence text that was generated.
            ps.setString(1, sentenceText);
            // sammy 5/2: store which generation algorithm produced the sentence.
            ps.setString(2, algorithmName);
            // sammy 5/2: keep null handling explicit when there was no resolved starting word id.
            if (startingWordId == null) {
                // sammy 5/2: write a SQL null instead of forcing a fake id.
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                // sammy 5/2: store the real starting word id when one exists.
                ps.setInt(3, startingWordId);
            }
            // sammy 5/2: execute the insert without suppressing duplicates because duplicates are valid history now.
            ps.executeUpdate();
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
