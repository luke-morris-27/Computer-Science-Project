/*
 * Class: AutocompleteDao
 * Created by: Sammy
 * Description: Retrieves weighted next-word suggestions and ensures unknown words are inserted.
 * Example: List<WeightedWord> list = dao.findNextWordSuggestions("hello", 5)
 */
package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import generator.AutocompleteGateway;
import generator.WeightedWord;
import parser.WordDb;

// reads and writes autocomplete data from the database
public class AutocompleteDao implements AutocompleteGateway {
    @Override
    // looks up next words ordered by highest transition count
    public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (normalizedWord == null || normalizedWord.isBlank()) {
            return List.of();
        }

        // opens the database and runs the autocomplete query
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT next_words.word_id, next_words.word_text, nw.transition_count " +
                     "FROM words current_words " +
                     "JOIN next_word nw ON nw.from_word_id = current_words.word_id " +
                     "JOIN words next_words ON next_words.word_id = nw.to_word_id " +
                     "WHERE current_words.word_text = ? " +
                     "ORDER BY nw.transition_count DESC, next_words.word_id ASC " +
                     "LIMIT ?"
             )) {
            ps.setString(1, normalizedWord);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                // saves the returned suggestions
                List<WeightedWord> suggestions = new ArrayList<>();

                // adds each database row as a weighted word result
                while (rs.next()) {
                    suggestions.add(new WeightedWord(
                        rs.getInt("word_id"),
                        rs.getString("word_text"),
                        rs.getInt("transition_count")
                    ));
                }
                return suggestions;
            }
        }
    }

    @Override
    // makes sure a word exists in the words table
    public void ensureWordExists(String normalizedWord) throws SQLException {
        if (normalizedWord == null || normalizedWord.isBlank()) {
            return;
        }

        // opens the database and inserts the word if missing
        try (Connection conn = WordDb.openConnection()) {
            WordDb.getOrCreateWordId(normalizedWord, conn);
        }
    }
}
