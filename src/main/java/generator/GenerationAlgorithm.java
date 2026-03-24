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
    WEIGHTED,

    // always picks the most likely next word
    GREEDY
}
