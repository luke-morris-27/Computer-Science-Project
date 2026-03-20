/*
 * Class: StreamingTokenProcessor
 * Created by: Person 3
 * Description: Processes tokens incrementally from a Reader and forwards each token to a callback.
 * Example: int paragraphs = processor.process(reader, tokenConsumer)
 */
package parser;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

public class StreamingTokenProcessor {
    private final Tokenizer tokenizer;

    public StreamingTokenProcessor() {
        this(new Tokenizer());
    }

    public StreamingTokenProcessor(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public int process(Reader reader, Consumer<String> tokenConsumer) throws IOException {
        // Guidance:
        // 1. Use tokenizer.tokenizeStreaming(reader).
        // 2. Send each token to tokenConsumer in order.
        // 3. Return the paragraph count from StreamResult.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
