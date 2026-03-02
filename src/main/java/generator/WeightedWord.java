package generator;

/*
 * Class: WeightedWord
 * Created by: Archisha Sasson
 * Description: Represents a candidate word for generator selection along with
 * its database ID and weight/frequency value.
 * Example: word_id=2, word_text="world", weight=5
 */

// Code by Archisha Sasson
public record WeightedWord(int wordId, String wordText, int weight) {
}
// End of Code by Archisha Sasson
