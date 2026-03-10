/*
 * Class: ReportsController
 * Created by: Person 5
 * Description: Handles report-screen queries for word lists and generated sentence history.
 * Example: controller.listWords(WordReportSort.ALPHABETICAL, 200)
 */
package ui;

import java.sql.SQLException;
import java.util.List;

public class ReportsController {
    private final UiReportingService reportingService;

    public ReportsController(UiReportingService reportingService) {
        this.reportingService = reportingService;
    }

    public List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException {
        // Guidance:
        // 1. Apply default sort when null.
        // 2. Apply safe default for non-positive limit.
        // 3. Delegate to reportingService.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException {
        // Guidance:
        // 1. Apply safe default for non-positive limit.
        // 2. Delegate to reportingService.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
