/*
 * Class: WordStatsAggregate
 * Created by: Person 2
 * Description: Holds per-file counts for one normalized word.
 * Example: new WordStatsAggregate(12, 2, 3)
 */
package parser;

public record WordStatsAggregate(int countInFile, int startInFile, int endInFile) {
}
