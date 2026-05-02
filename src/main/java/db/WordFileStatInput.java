/*
 * Class: WordFileStatInput
 * Created by: Shriram Janardhan
 * Description: Represents one row payload for the word_file_stats table.
 * Example: new WordFileStatInput(wordId, countInFile, startInFile, endInFile)
 */
package db;

// Code by Shriram Janardhan
public record WordFileStatInput(int wordId, int countInFile, int startInFile, int endInFile) {
}
// End of code by Shriram Janardhan (WordFileStatInput record)
