/*
 * Class: WordReportView
 * Created by: Archisha Sasson
 * Description: Represents one row in the UI word-report table.
 * Example: new WordReportView("hello", 20, 4, 3)
 */
package ui;

public record WordReportView(String wordText, int totalCount, int startCount, int endCount) {
}
