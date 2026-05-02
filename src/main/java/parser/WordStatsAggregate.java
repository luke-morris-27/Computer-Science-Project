/*
 * Class: WordStatsAggregate
 * Created by: Shriram Janardhan
 * Description: Holds per-file counts for one normalized word.
 * Example: new WordStatsAggregate(12, 2, 3)
 */
package parser;

// Code by Shriram Janardhan
public record WordStatsAggregate(int countInFile, int startInFile, int endInFile) {
}
// End of code by Shriram Janardhan (WordStatsAggregate record)
