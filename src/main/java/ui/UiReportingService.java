/*
 * Class: UiReportingService
 * Created by: Archisha Sasson
 * Description: Supplies reporting data used by the reports screen.
 * Example: service.listGeneratedSentences(true, 100)
 */
package ui;

import java.sql.SQLException;
import java.util.List;

public interface UiReportingService {
    List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException;

    default List<WordReportView> listWords(WordReportSort sort, int limit, String searchText) throws SQLException {
        return listWords(sort, limit);
    }

    List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException;
}
