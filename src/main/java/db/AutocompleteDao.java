/*
 * Class: AutocompleteDao
 * Created by: Person 4
 * Description: Retrieves weighted next-word suggestions and ensures unknown words are inserted.
 * Example: List<WeightedWord> list = dao.findNextWordSuggestions("hello", 5)
 */
package db;

import java.sql.SQLException;
import java.util.List;

import generator.AutocompleteGateway;
import generator.WeightedWord;

public class AutocompleteDao implements AutocompleteGateway {
    @Override
    public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException {
        // Guidance:
        // Join words -> next_word -> words and return transitions ordered by highest transition_count.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void ensureWordExists(String normalizedWord) throws SQLException {
        // Guidance:
        // Insert word when missing, otherwise no-op.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
