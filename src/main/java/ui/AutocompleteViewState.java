/*
 * Class: AutocompleteViewState
 * Created by: Archisha Sasson
 * Description: Encapsulates whether suggestions were requested and what suggestions should be shown.
 * Example: new AutocompleteViewState(true, List.of("world", "there"))
 */
package ui;

import java.util.List;

public record AutocompleteViewState(boolean suggestionsRequested, List<String> suggestions) {
}
