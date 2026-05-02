/*
 * Class: GenerationAlgorithm
 * Created by: Sammy
 * Modified by: Omesh Sana
 * Description: Defines which sentence-generation strategy should be used for a request.
 * Example: GenerationAlgorithm.GREEDY
 */
package generator;

// lists the allowed generation modes
public enum GenerationAlgorithm {
    // picks words based on weighted frequency
    WEIGHTED("Varied - Words are chosen proportionally to frequency"),

    // always picks the most likely next word
    GREEDY("Consistent - Always picks the single most common next word"),

    RANDOM("Random - Picks any observed next word uniformly");

    private final String displayName;

    GenerationAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
