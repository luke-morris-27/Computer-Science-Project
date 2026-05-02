package parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Class: ProgressTrackerTest
 * Created by: Archisha Sasson
 * Description: Verifies progress tracker counting, percentage calculation, and formatted output.
 */
@Tag("unit")
@Tag("task1-person3")
@DisplayName("Progress Tracker Tests")
class ProgressTrackerTest {
    private final ProgressTracker tracker = new ProgressTracker();

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies progress calculations and formatting.");
    }

    @Test
    @DisplayName("Percent is clamped between 0 and 100")
    void percentIsClamped() {
        assertEquals(0, tracker.percent(-1, 100));
        assertEquals(50, tracker.percent(50, 100));
        assertEquals(100, tracker.percent(120, 100));
        assertEquals(0, tracker.percent(1, 0));
    }

    @Test
    @DisplayName("Render bar uses filled segments from computed percent")
    void renderBarUsesPercent() {
        assertEquals("[#####-----] 50%", tracker.renderBar(50, 100, 10));
        assertEquals("[----------] 0%", tracker.renderBar(0, 100, 10));
    }
}
