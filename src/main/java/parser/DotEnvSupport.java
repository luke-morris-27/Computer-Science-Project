package parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a optional {@code .env} file from the JVM working directory (usually the project root
 * when you run {@code mvn javafx:run}). Same keys as OS environment variables
 * (e.g. {@link DatabaseConfig#PROP_JDBC_URL}). Lines are {@code KEY=value}; {@code #} starts a comment.
 */
final class DotEnvSupport {
    private static volatile Map<String, String> loaded;
    private static final Object LOCK = new Object();

    private DotEnvSupport() {
    }

    /**
     * Returns the value for {@code key} from {@code .env}, or {@code null} if the file is missing
     * or the key is not set. An explicit empty value {@code KEY=} yields {@code ""}.
     */
    static String get(String key) {
        Map<String, String> map = loadIfNeeded();
        if (!map.containsKey(key)) {
            return null;
        }
        return map.get(key);
    }

    private static Map<String, String> loadIfNeeded() {
        if (loaded != null) {
            return loaded;
        }
        synchronized (LOCK) {
            if (loaded != null) {
                return loaded;
            }
            Path envFile = Paths.get(System.getProperty("user.dir"), ".env");
            Map<String, String> map = new HashMap<>();
            if (Files.isRegularFile(envFile)) {
                try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eq = trimmed.indexOf('=');
                        if (eq <= 0) {
                            continue;
                        }
                        String k = trimmed.substring(0, eq).trim();
                        String v = trimmed.substring(eq + 1).trim();
                        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                            v = v.substring(1, v.length() - 1);
                        }
                        map.put(k, v);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read .env at " + envFile.toAbsolutePath(), e);
                }
            }
            loaded = Collections.unmodifiableMap(map);
            return loaded;
        }
    }
}
