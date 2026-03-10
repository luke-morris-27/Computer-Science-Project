/*
 * Class: GenerateController
 * Created by: Person 5
 * Description: Handles generation-screen requests and maps service outcomes into UI view state.
 * Example: controller.generate(GenerationAlgorithm.GREEDY, "hello", 10)
 */
package ui;

import generator.GenerationAlgorithm;
import generator.GenerationService;

public class GenerateController {
    private final GenerationService generationService;

    public GenerateController(GenerationService generationService) {
        this.generationService = generationService;
    }

    public GenerateViewState generate(GenerationAlgorithm algorithm, String startWord, int maxWords) {
        // Guidance:
        // 1. Validate algorithm and maxWords.
        // 2. Call generationService.
        // 3. Convert success/failure into GenerateViewState.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
