/*
 * Class: WordFileStatInput
 * Created by: Person 2
 * Description: Represents one row payload for the word_file_stats table.
 * Example: new WordFileStatInput(wordId, countInFile, startInFile, endInFile)
 */
package db;

public record WordFileStatInput(int wordId, int countInFile, int startInFile, int endInFile) {
}
