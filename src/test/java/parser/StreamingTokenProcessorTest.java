package parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Class: StreamingTokenProcessorTest
 * Created by: Archisha Sasson
 * Description: Verifies streaming token processor forwarding, buffering, and token-boundary handling.
 */
@Tag("unit")
@Tag("task1-person3")
@DisplayName("Streaming Token Processor Tests")
class StreamingTokenProcessorTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies streaming token forwarding.");
    }

    @Test
    @DisplayName("Process forwards tokens and paragraph count")
    void processStreamsTokensAndParagraphCount() throws Exception {
        StreamingTokenProcessor processor = new StreamingTokenProcessor();
        List<String> seen = new ArrayList<>();

        int paragraphCount = processor.process(
            new StringReader("Alpha beta.\n\nGamma delta."),
            seen::add
        );

        assertEquals(2, paragraphCount);
        assertEquals(List.of(
            "Alpha", "beta", Tokenizer.SENTENCE_BOUNDARY,
            "Gamma", "delta", Tokenizer.SENTENCE_BOUNDARY
        ), seen);
    }
}
