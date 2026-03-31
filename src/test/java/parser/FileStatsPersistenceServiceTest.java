package parser;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Class: FileStatsPersistenceServiceTest
 * Created by: Archisha Sasson
 * Description: Verifies file-stats aggregation is translated into the correct persistence calls.
 */
@Tag("unit")
@Tag("task1-person2")
@DisplayName("File Stats Persistence Service Unit Tests")
class FileStatsPersistenceServiceTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies word stats aggregation rules.");
    }

    @Test
    @DisplayName("Build word stats map merges word/start/end counts")
    void buildWordStatsMergesAllCounts() {
        ParseResult result = new ParseResult();
        result.incrementWordCount("alpha");
        result.incrementWordCount("alpha");
        result.incrementWordCount("beta");
        result.incrementSentenceStartCount("alpha");
        result.incrementSentenceEndCount("beta");

        Map<String, WordStatsAggregate> aggregates = FileStatsPersistenceService.buildWordStats(result);

        assertEquals(2, aggregates.get("alpha").countInFile());
        assertEquals(1, aggregates.get("alpha").startInFile());
        assertEquals(0, aggregates.get("alpha").endInFile());

        assertEquals(1, aggregates.get("beta").countInFile());
        assertEquals(0, aggregates.get("beta").startInFile());
        assertEquals(1, aggregates.get("beta").endInFile());
    }
}
// End of code by Shriram Janardhan (FileStatsPersistenceServiceTest)
