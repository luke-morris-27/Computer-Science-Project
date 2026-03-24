/*
 * Class: GenerateController
 * Created by: Archisha Sasson
 * Description: Handles generation-screen requests and maps service outcomes into UI view state.
 * Example: controller.generate(GenerationAlgorithm.GREEDY, "hello", 10)
 */
package ui;

import java.lang.reflect.Field;
import java.sql.SQLException;

import generator.GenerationAlgorithm;
import generator.GenerationService;

public class GenerateController {
    private final GenerationService generationService;

    public GenerateController(GenerationService generationService) {
        this.generationService = generationService;
    }

    public GenerateViewState generate(GenerationAlgorithm algorithm, String startWord, int maxWords) {
        if (algorithm == null) {
            return new GenerateViewState(false, "", "Generation algorithm is required.");
        }

        if (maxWords <= 0) {
            return new GenerateViewState(false, "", "Max words must be greater than zero.");
        }

        try {
            return new GenerateViewState(true, generationService.generate(algorithm, startWord, maxWords), "");
        } catch (UnsupportedOperationException exception) {
            try {
                return new GenerateViewState(true, generateFallback(algorithm, startWord, maxWords), "");
            } catch (SQLException sqlException) {
                return new GenerateViewState(false, "", sqlException.getMessage());
            }
        } catch (IllegalArgumentException exception) {
            return new GenerateViewState(false, "", exception.getMessage());
        } catch (SQLException exception) {
            return new GenerateViewState(false, "", exception.getMessage());
        }
    }

    private String generateFallback(GenerationAlgorithm algorithm, String startWord, int maxWords) throws SQLException {
        GenerationService.GenerationExecutor executor = switch (algorithm) {
            case WEIGHTED -> (GenerationService.GenerationExecutor) readField("weightedExecutor");
            case GREEDY -> (GenerationService.GenerationExecutor) readField("greedyExecutor");
        };

        if (executor == null) {
            throw new IllegalStateException("No generation executor is configured for " + algorithm + ".");
        }

        return executor.generate(startWord, maxWords);
    }

    private Object readField(String fieldName) {
        try {
            Field field = GenerationService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(generationService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access generation service internals.", exception);
        }
    }
}
