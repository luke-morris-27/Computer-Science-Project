package parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Central JDBC settings for MySQL/MariaDB. Values are resolved in order:
 * <ol>
 *   <li>JVM system properties {@value #PROP_JDBC_URL}, {@value #PROP_DB_USER}, {@value #PROP_DB_PASSWORD}</li>
 *   <li>Environment variables with the same names</li>
 *   <li>Project root {@code .env} (see {@code .env.example} in the repo) — recommended for local passwords; never commit {@code .env}</li>
 *   <li>Optional classpath file {@code /database.properties} (see {@code database.properties.example})</li>
 *   <li>{@link #DEFAULT_JDBC_URL}, {@link #DEFAULT_USER}, {@link #DEFAULT_PASSWORD}</li>
 * </ol>
 * <p>
 * The application expects a running MySQL or MariaDB server and a schema created from
 * {@code database/SentenceBuilderDatabase.sql} (and optional seed from {@code database/insertData.sql}).
 */
public final class DatabaseConfig {

    /** System property / env var for JDBC URL (overrides everything except explicit code). */
    public static final String PROP_JDBC_URL = "SENTENCE_BUILDER_DB_URL";
    /** System property / env var for database username. */
    public static final String PROP_DB_USER = "SENTENCE_BUILDER_DB_USER";
    /** System property / env var for database password. */
    public static final String PROP_DB_PASSWORD = "SENTENCE_BUILDER_DB_PASSWORD";

    /** Keys inside optional {@code database.properties} on the classpath. */
    public static final String FILE_KEY_JDBC_URL = "jdbc.url";
    public static final String FILE_KEY_DB_USER = "jdbc.user";
    public static final String FILE_KEY_DB_PASSWORD = "jdbc.password";

    /**
     * Default URL: local MySQL on port 3306, database {@code sentence_builder}, matching {@code database/SentenceBuilderDatabase.sql}.
     */
    public static final String DEFAULT_JDBC_URL =
        "jdbc:mysql://localhost:3306/sentence_builder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";

    private static final String PROPERTIES_RESOURCE = "/database.properties";
    private static volatile Properties classpathProperties;
    private static final Object PROPERTIES_LOCK = new Object();

    private DatabaseConfig() {
    }

    /**
     * Loads {@code database.properties} from the classpath once, if present. Missing file yields empty properties.
     */
    public static Properties classpathDatabaseProperties() {
        if (classpathProperties != null) {
            return classpathProperties;
        }
        synchronized (PROPERTIES_LOCK) {
            if (classpathProperties != null) {
                return classpathProperties;
            }
            Properties loaded = new Properties();
            try (InputStream in = DatabaseConfig.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
                if (in != null) {
                    loaded.load(in);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load " + PROPERTIES_RESOURCE, e);
            }
            classpathProperties = loaded;
            return classpathProperties;
        }
    }

    public static String resolveJdbcUrl() {
        String fromJvm = firstNonBlank(
            System.getProperty(PROP_JDBC_URL),
            System.getenv(PROP_JDBC_URL)
        );
        if (fromJvm != null) {
            return fromJvm;
        }
        String fromDotEnv = DotEnvSupport.get(PROP_JDBC_URL);
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv.trim();
        }
        String fromFile = classpathDatabaseProperties().getProperty(FILE_KEY_JDBC_URL);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return DEFAULT_JDBC_URL;
    }

    public static String resolveUsername() {
        String fromJvm = firstNonBlank(
            System.getProperty(PROP_DB_USER),
            System.getenv(PROP_DB_USER)
        );
        if (fromJvm != null) {
            return fromJvm;
        }
        String fromDotEnv = DotEnvSupport.get(PROP_DB_USER);
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv.trim();
        }
        String fromFile = classpathDatabaseProperties().getProperty(FILE_KEY_DB_USER);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return DEFAULT_USER;
    }

    public static String resolvePassword() {
        String fromJvm = firstNonBlank(
            System.getProperty(PROP_DB_PASSWORD),
            System.getenv(PROP_DB_PASSWORD)
        );
        if (fromJvm != null) {
            return fromJvm;
        }
        String fromDotEnv = DotEnvSupport.get(PROP_DB_PASSWORD);
        if (fromDotEnv != null) {
            return fromDotEnv;
        }
        if (classpathDatabaseProperties().containsKey(FILE_KEY_DB_PASSWORD)) {
            return classpathDatabaseProperties().getProperty(FILE_KEY_DB_PASSWORD, "");
        }
        return DEFAULT_PASSWORD;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
