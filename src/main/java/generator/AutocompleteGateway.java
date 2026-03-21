/*
 * Class: AutocompleteGateway
 * Created by: Sammy
 * Description: Provides persistence operations needed by autocomplete logic.
 * Example: gateway.findNextWordSuggestions("hello", 5)
 */
package generator;

import java.sql.SQLException;
import java.util.List;

// lets different data sources provide autocomplete results
public interface AutocompleteGateway {
    // finds next word suggestions for a cleaned word
    List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException;

    // makes sure a word exists in storage
    void ensureWordExists(String normalizedWord) throws SQLException;
}
