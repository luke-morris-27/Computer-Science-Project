package parser;

import java.util.Objects;

public final class TransitionKey {
    private final String fromWord;
    private final String toWord;

    public TransitionKey(String fromWord, String toWord) {
        this.fromWord = fromWord == null ? "" : fromWord;
        this.toWord = toWord == null ? "" : toWord;
    }

    public String fromWord() {
        return fromWord;
    }

    public String toWord() {
        return toWord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransitionKey that)) return false;
        return Objects.equals(fromWord, that.fromWord) && Objects.equals(toWord, that.toWord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromWord, toWord);
    }
}

