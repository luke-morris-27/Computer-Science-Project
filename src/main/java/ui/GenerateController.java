/*
 * Class: GenerateController
 * Created by: Archisha Sasson
 * Description: Handles generation-screen requests and maps service outcomes into UI view state.
 * Example: controller.generate(GenerationAlgorithm.GREEDY, "hello", 10)
 */
package ui;

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
        } catch (IllegalArgumentException exception) {
            return new GenerateViewState(false, "", exception.getMessage());
        } catch (SQLException exception) {
            return new GenerateViewState(false, "", exception.getMessage());
        }
    }
}
