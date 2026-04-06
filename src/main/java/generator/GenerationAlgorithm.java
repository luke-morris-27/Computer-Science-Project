/*
 * Class: GenerationAlgorithm
 * Created by: Sammy
 * Description: Defines which sentence-generation strategy should be used for a request.
 * Example: GenerationAlgorithm.GREEDY
 */
package generator;

// lists the allowed generation modes
public enum GenerationAlgorithm {
    // picks words based on weighted frequency
    WEIGHTED("Words are chosen proportionally to frequency"),

    // always picks the most likely next word
    GREEDY("Always picks the single most common next word");

    // Code by Shriram
    // stores the user-friendly label shown in the UI
    private final String displayName;

    // assigns the display name for each algorithm option
    GenerationAlgorithm(String displayName) {
        this.displayName = displayName;
    }

    // returns the user-friendly label so combo boxes show readable text
    @Override
    public String toString() {
        return displayName;
    }
    // End of Code by Shriram
}
