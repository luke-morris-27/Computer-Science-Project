/*
 * Class: ProgressTracker
 * Created by: Luke Morris
 * Description: Calculates progress percentages and renders text progress bars for long-running imports.
 * Example: String bar = tracker.renderBar(25, 100, 20)
 */
package parser;

public class ProgressTracker {

    /*
     * Calculates percentage progress.
     *
     * Parameters:
     * - current: current progress value
     * - total: total expected value
     *
     * Returns:
     * - percentage between 0 and 100
     */
    public int percent(int current, int total) {
        if (total <= 0) {
            return 0;
        }
        // Calculate percentage
        int percent = (int) ((current * 100.0) / total);
        // Clamp value to [0, 100]
        if (percent < 0) return 0;
        if (percent > 100) return 100;

        return percent;
    }

    /*
     * Renders a progress bar string.
     *
     * Example output:
     * [#####-----] 50%
     *
     * Parameters:
     * - current: current progress
     * - total: total progress
     * - width: number of characters in the bar
     */
    public String renderBar(int current, int total, int width) {

        int percent = percent(current, total);

        int filled = (int) ((percent / 100.0) * width);
        int empty = width - filled;

        StringBuilder bar = new StringBuilder();

        bar.append("[");

        for (int i = 0; i < filled; i++) {
            bar.append("#");
        }

        for (int i = 0; i < empty; i++) {
            bar.append("-");
        }

        bar.append("] ");
        bar.append(percent).append("%");

        return bar.toString();
    }
}
