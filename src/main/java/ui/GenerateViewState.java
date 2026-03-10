/*
 * Class: GenerateViewState
 * Created by: Person 5
 * Description: Encapsulates generation-screen result, including success state, sentence output, and error text.
 * Example: new GenerateViewState(true, "hello world", "")
 */
package ui;

public record GenerateViewState(boolean success, String sentence, String errorMessage) {
}
