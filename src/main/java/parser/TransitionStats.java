package parser;

public final class TransitionStats {
    private int count;
    private boolean followsSentenceStart;
    private boolean precedesSentenceEnd;

    public void increment(boolean followsStart) {
        count++;
        followsSentenceStart = followsSentenceStart || followsStart;
    }

    public void markPrecedesEnd() {
        precedesSentenceEnd = true;
    }

    public int count() {
        return count;
    }

    public boolean followsSentenceStart() {
        return followsSentenceStart;
    }

    public boolean precedesSentenceEnd() {
        return precedesSentenceEnd;
    }
}
