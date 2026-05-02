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

        // Code by Shriram
        // opens the database and runs the autocomplete query
        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT next_words.word_id, next_words.word_text, nw.transition_count " +
                     "FROM words current_words " +
                     "JOIN next_word nw ON nw.from_word_id = current_words.word_id " +
                     "JOIN words next_words ON next_words.word_id = nw.to_word_id " +
                     "WHERE current_words.word_text = ? " +
                     "ORDER BY nw.transition_count DESC, next_words.word_text ASC " +
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
        // End of Code by Shriram
    }

    @Override
    // makes sure a word exists in the words table
    public void ensureWordExists(String normalizedWord) throws SQLException {
        // sammy 5/2: ignore empty values so we do not create meaningless word rows.
        if (normalizedWord == null || normalizedWord.isBlank()) {
            // sammy 5/2: there is nothing valid to persist for a blank word.
            return;
        }

        // sammy 5/2: open one connection so the word lookup and user-input tracking stay together.
        try (Connection conn = WordDb.openConnection()) {
            // sammy 5/2: use one small transaction so the word row and user-input metadata stay consistent.
            conn.setAutoCommit(false);
            try {
                // sammy 5/2: create the word if it is new or reuse the existing word id if it is already known.
                int wordId = WordDb.getOrCreateWordId(normalizedWord, conn);
                // sammy 5/2: record that this word came through the user-input autocomplete flow.
                recordUserInputWord(conn, wordId);
                // sammy 5/2: refresh the reusable word row with a recent seen timestamp.
                updateWordLastSeen(conn, wordId);
                // sammy 5/2: commit both metadata updates together.
                conn.commit();
            } catch (SQLException exception) {
                // sammy 5/2: roll back the small transaction if either tracking step fails.
                conn.rollback();
                // sammy 5/2: rethrow so the ui can surface the database failure.
                throw exception;
            }
        }
    }

    // sammy 5/2: add a history row so user-entered words are actually represented in the database metadata layer.
    private void recordUserInputWord(Connection conn, int wordId) throws SQLException {
        // sammy 5/2: insert one user_input_words row for this user-entered word event.
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO user_input_words (word_id) VALUES (?)"
        )) {
            // sammy 5/2: tie the event row back to the canonical words row.
            ps.setInt(1, wordId);
            // sammy 5/2: execute the event insert immediately inside the current transaction.
            ps.executeUpdate();
        }
    }

    // sammy 5/2: update the word row so existing metadata reflects that the word was touched recently.
    private void updateWordLastSeen(Connection conn, int wordId) throws SQLException {
        // sammy 5/2: stamp the word row with the current database time for later inspection/reporting.
        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE words SET last_seen_at = CURRENT_TIMESTAMP WHERE word_id = ?"
        )) {
            // sammy 5/2: target the exact canonical word row we just inserted or reused.
            ps.setInt(1, wordId);
            // sammy 5/2: apply the metadata update as part of the same transaction.
            ps.executeUpdate();
        }
    }
}
