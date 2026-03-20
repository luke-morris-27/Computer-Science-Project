package ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import generator.GenerationAlgorithm;
import generator.GenerationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Tests for Task 2, Person 5.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("Generate Controller Tests")
class GenerateControllerTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies generation controller request handling.");
    }

    @Test
    @DisplayName("Generate returns success state on valid request")
    void validGenerateRequestReturnsSuccessState() {
        GenerationService service = new GenerationService(
            (start, max) -> "weighted-sentence",
            (start, max) -> "greedy-sentence"
        );
        GenerateController controller = new GenerateController(service);

        GenerateViewState state = controller.generate(GenerationAlgorithm.GREEDY, "hello", 5);

        assertTrue(state.success());
        assertEquals("greedy-sentence", state.sentence());
    }

    @Test
    @DisplayName("Generate rejects invalid max words")
    void invalidMaxWordsReturnsErrorState() {
        GenerationService service = new GenerationService(
            (start, max) -> "weighted-sentence",
            (start, max) -> "greedy-sentence"
        );
        GenerateController controller = new GenerateController(service);

        GenerateViewState state = controller.generate(GenerationAlgorithm.GREEDY, "hello", 0);

        assertEquals(false, state.success());
        assertEquals("Max words must be greater than zero.", state.errorMessage());
    }
}
