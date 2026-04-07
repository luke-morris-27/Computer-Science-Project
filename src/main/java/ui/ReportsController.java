/*
 * Class: ReportsController
 * Created by: Archisha Sasson
 * Description: Handles report-screen queries for word lists and generated sentence history.
 * Example: controller.listWords(WordReportSort.ALPHABETICAL, 200)
 */
package ui;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class ReportsController {
    private final UiReportingService reportingService;

    public ReportsController(UiReportingService reportingService) {
        this.reportingService = reportingService;
    }

    public List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException {
        WordReportSort effectiveSort = sort == null ? WordReportSort.ALPHABETICAL : sort;
        int effectiveLimit = limit <= 0 ? 100 : limit;
        return reportingService.listWords(effectiveSort, effectiveLimit);
    }

    public List<WordReportView> listWords(WordReportSort sort, int limit, String searchText) throws SQLException {
        WordReportSort effectiveSort = sort == null ? WordReportSort.ALPHABETICAL : sort;
        int effectiveLimit = limit <= 0 ? 100 : limit;
        String effectiveSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        return reportingService.listWords(effectiveSort, effectiveLimit, effectiveSearch);
    }

    public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException {
        int effectiveLimit = limit <= 0 ? 100 : limit;
        return reportingService.listGeneratedSentences(onlyDuplicates, effectiveLimit);
    }
}
