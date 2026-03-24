/*
 * Class: GenerationService
 * Created by: Sammy
 * Description: Routes generation requests to weighted or greedy engines and enforces request validation.
 *              Basically the entry point for sentence generation from UI side.
 * Purpose: So that the UI/controller can just call generationService.generate(algorithm, startWord, maxWords) instead 
 *          of knowing which to call, how to validate input, or how weighted vs. greedy works.
 * Example: String sentence = service.generate(GenerationAlgorithm.WEIGHTED, "hello", 15)
 */
package generator;

import java.sql.SQLException;

// routes generation requests to the right algorithm
public class GenerationService { 
    @FunctionalInterface
    // defines one generation function shape
    public interface GenerationExecutor {
        // if we add another generation style later, this helps it adapt more easily
        String generate(String startWord, int maxWords) throws SQLException;
    }

    // both private because other classes should not directly modify these
    // final so that once service is created, can't change it accidentally
    // stores the weighted generator action
    private final GenerationExecutor weightedExecutor;

    // stores the greedy generator action
    private final GenerationExecutor greedyExecutor;

    // builds the service with the real generators
    public GenerationService() {
        // creates the weighted generator
        WeightedGenerator weightedGenerator = new WeightedGenerator();

        // creates the greedy generator
        GreedyGenerator greedyGenerator = new GreedyGenerator();
        this.weightedExecutor = weightedGenerator::generateWeighted;
        this.greedyExecutor = greedyGenerator::generateGreedy;
    }

    // builds the service with custom executors for testing
    public GenerationService(GenerationExecutor weightedExecutor, GenerationExecutor greedyExecutor) {
        this.weightedExecutor = weightedExecutor;
        this.greedyExecutor = greedyExecutor;
    }

    // sends the request to weighted or greedy generation
    public String generate(GenerationAlgorithm algorithm, String startWord, int maxWords) throws SQLException {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        if (maxWords <= 0) {
            throw new IllegalArgumentException("maxWords must be > 0");
        }

        return switch (algorithm) {
            case WEIGHTED -> {
                if (weightedExecutor == null) {
                    throw new IllegalStateException("weightedExecutor is not configured");
                }
                yield weightedExecutor.generate(startWord, maxWords);
            }
            case GREEDY -> {
                if (greedyExecutor == null) {
                    throw new IllegalStateException("greedyExecutor is not configured");
                }
                yield greedyExecutor.generate(startWord, maxWords);
            }
        };
    }
}
