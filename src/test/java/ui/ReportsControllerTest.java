package ui;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Tests for Task 2, Person 5.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Reports Controller Tests")
class ReportsControllerTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies reports controller defaults and delegation.");
    }

    @Test
    @DisplayName("Null sort falls back to ALPHABETICAL")
    void nullSortFallsBackToAlphabetical() throws Exception {
        FakeReportingService reportingService = new FakeReportingService();
        ReportsController controller = new ReportsController(reportingService);

        controller.listWords(null, 25);

        assertEquals(WordReportSort.ALPHABETICAL, reportingService.lastSort);
        assertEquals(25, reportingService.lastLimit);
    }

    @Test
    @DisplayName("Non-positive limit falls back to 100")
    void nonPositiveLimitFallsBackToDefault() throws Exception {
        FakeReportingService reportingService = new FakeReportingService();
        ReportsController controller = new ReportsController(reportingService);

        controller.listGeneratedSentences(true, 0);

        assertEquals(true, reportingService.lastOnlyDuplicates);
        assertEquals(100, reportingService.lastLimit);
    }

    private static final class FakeReportingService implements UiReportingService {
        private WordReportSort lastSort;
        private int lastLimit;
        private boolean lastOnlyDuplicates;

        @Override
        public List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException {
            this.lastSort = sort;
            this.lastLimit = limit;
            return List.of(new WordReportView("alpha", 10, 2, 1));
        }

        @Override
        public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException {
            this.lastOnlyDuplicates = onlyDuplicates;
            this.lastLimit = limit;
            return List.of("alpha beta");
        }
    }
}
