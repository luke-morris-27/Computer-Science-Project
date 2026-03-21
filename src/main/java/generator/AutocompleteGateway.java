/*
 * Class: AutocompleteGateway
 * Created by: Sammy
 * Description: Provides persistence operations needed by autocomplete logic.
 * Example: gateway.findNextWordSuggestions("hello", 5)
 */
package generator;

import java.sql.SQLException;
import java.util.List;

public interface AutocompleteGateway {
    List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) throws SQLException;

    void ensureWordExists(String normalizedWord) throws SQLException;
}
