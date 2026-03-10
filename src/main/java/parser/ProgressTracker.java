/*
 * Class: ProgressTracker
 * Created by: Person 3
 * Description: Calculates progress percentages and renders text progress bars for long-running imports.
 * Example: String bar = tracker.renderBar(25, 100, 20)
 */
package parser;

public class ProgressTracker {
    public int percent(int current, int total) {
        // Guidance:
        // Return a value in [0, 100]. Handle total <= 0 safely.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String renderBar(int current, int total, int width) {
        // Guidance:
        // Use percent() to determine fill amount.
        // Output format should look like: [#####-----] 50%
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
