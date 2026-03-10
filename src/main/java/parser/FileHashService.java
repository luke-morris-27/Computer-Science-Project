/*
 * Class: FileHashService
 * Created by: Person 1
 * Description: Computes a deterministic SHA-256 hash for a file so duplicate imports can be detected by content.
 * Example: String hash = new FileHashService().sha256(Path.of("book.txt"));
 */
package parser;

import java.io.IOException;
import java.nio.file.Path;

public class FileHashService {
    public String sha256(Path file) throws IOException {
        // Guidance:
        // 1. Validate input path is non-null and points to a regular file.
        // 2. Stream file bytes in chunks into a SHA-256 MessageDigest.
        // 3. Return the digest as a lowercase 64-character hex string.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
