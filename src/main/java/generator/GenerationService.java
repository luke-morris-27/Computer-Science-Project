/*
 * Class: GenerationService
 * Created by: Person 4
 * Description: Routes generation requests to weighted or greedy engines and enforces request validation.
 * Example: String sentence = service.generate(GenerationAlgorithm.WEIGHTED, "hello", 15)
 */
package generator;

import java.sql.SQLException;

public class GenerationService {
    @FunctionalInterface
    public interface GenerationExecutor {
        String generate(String startWord, int maxWords) throws SQLException;
    }

    private final GenerationExecutor weightedExecutor;
    private final GenerationExecutor greedyExecutor;

    public GenerationService() {
        this.weightedExecutor = null;
        this.greedyExecutor = null;
    }

    public GenerationService(GenerationExecutor weightedExecutor, GenerationExecutor greedyExecutor) {
        this.weightedExecutor = weightedExecutor;
        this.greedyExecutor = greedyExecutor;
    }

    public String generate(GenerationAlgorithm algorithm, String startWord, int maxWords) throws SQLException {
        // Guidance:
        // 1. Validate algorithm is present and maxWords > 0.
        // 2. Route to weightedExecutor or greedyExecutor by algorithm.
        // 3. Return generated sentence exactly as produced by the executor.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
