/*
 * Class: FileHashService
 * Created by: Person 1
 * Description: Computes a deterministic SHA-256 hash for a file so duplicate imports can be detected by content.
 * Example: String hash = new FileHashService().sha256(Path.of("book.txt"));
 */
package parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashService {
        public String sha256(Path file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("file must exist and be a regular file: " + file);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream in = Files.newInputStream(file);
                 DigestInputStream dis = new DigestInputStream(in, digest)) {

                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // reading updates the digest
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

}
