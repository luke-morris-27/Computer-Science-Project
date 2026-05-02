package generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/*
* Class: GenerationServiceTest
* Created by: Archisha Sasson
* Modified by: Omesh Sana
* Description: Verifies generation service routing between weighted, greedy,
* and random execution plus request validation.
*/
@Tag("unit")
@Tag("task2-person4")
@DisplayName("Generation Service Tests")
class GenerationServiceTest {

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName()
            + " | Verifies generation routing and validation.");
    }

    @Test
    @DisplayName("Routes weighted requests to weighted executor")
    void routesToWeightedExecutor() throws Exception {
        GenerationService service = new GenerationService(
            (start, maxWords) -> "weighted:" + start + ":" + maxWords,
            (start, maxWords) -> "greedy:" + start + ":" + maxWords,
            (start, maxWords) -> "random:" + start + ":" + maxWords
        );

        String sentence = service.generate(GenerationAlgorithm.WEIGHTED, "hello", 4);

        assertEquals("weighted:hello:4", sentence);
    }

    @Test
    @DisplayName("Routes random requests to random executor")
    void routesToRandomExecutor() throws Exception {
        GenerationService service = new GenerationService(
            (start, maxWords) -> "weighted:" + start + ":" + maxWords,
            (start, maxWords) -> "greedy:" + start + ":" + maxWords,
            (start, maxWords) -> "random:" + start + ":" + maxWords
        );

        String sentence = service.generate(GenerationAlgorithm.RANDOM, "hello", 4);

        assertEquals("random:hello:4", sentence);
    }

    @Test
    @DisplayName("Rejects non-positive maxWords")
    void rejectsInvalidMaxWords() {
        GenerationService service = new GenerationService(
            (start, maxWords) -> "weighted",
            (start, maxWords) -> "greedy",
            (start, maxWords) -> "random"
        );

        assertThrows(IllegalArgumentException.class,
            () -> service.generate(GenerationAlgorithm.GREEDY, "hello", 0));
    }
}