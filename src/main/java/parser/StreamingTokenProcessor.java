/*
 * Class: StreamingTokenProcessor
 * Created by: Luke Morris
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

    /*
     * Processes text from a Reader and sends each token to tokenConsumer.
     *
     * Parameters:
     * - reader: source of text (file, stream, etc.)
     * - tokenConsumer: function that handles each token
     *
     * Returns:
     * - number of paragraphs detected
     */
     public int process(Reader reader, Consumer<String> tokenConsumer) throws IOException {
        
         // 1. Tokenize the input using streaming tokenizer
        Tokenizer.StreamResult result = tokenizer.tokenizeStreaming(reader);

        // 2. Send tokens one-by-one
        for (String token : result.tokens) {
            tokenConsumer.accept(token);
        }

        // 3. Return paragraph count
        return result.paragraphCount;
    }
}
