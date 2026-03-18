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

        // 1. Get streaming result from tokenizer
        Tokenizer.StreamResult result = tokenizer.tokenizeStreaming(reader);

        int paragraphCount = result.getParagraphCount();

        // 2. Stream tokens one-by-one
        while (result.hasNext()) {
            String token = result.next();
            tokenConsumer.accept(token);
        }

        // 3. Return paragraph count
        return paragraphCount;
    }
}
