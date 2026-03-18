/*
 * Class: ProgressTracker
 * Created by: Person 3
 * Description: Calculates progress percentages and renders text progress bars for long-running imports.
 * Example: String bar = tracker.renderBar(25, 100, 20)
 */
package parser;

public class ProgressTracker {

    public int percent(int current, int total) {
        if (total <= 0) {
            return 0;
        }

        int percent = (int) ((current * 100.0) / total);

        if (percent < 0) return 0;
        if (percent > 100) return 100;

        return percent;
    }

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
