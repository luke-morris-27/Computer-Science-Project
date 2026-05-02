package support;

/*
 * Class: DatabaseIntegrationTestSupport
 * Created by: Archisha Sasson
 * Description: Provides shared setup utilities for ParserDB and generator
 * database integration tests, including schema reset and query helpers.
 * Example: Resets the test schema before each integration test against MySQL.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import parser.WordDb;

// Code by Archisha Sasson
public abstract class DatabaseIntegrationTestSupport {
    private static final String DB_URL_PROPERTY = "SENTENCE_BUILDER_DB_URL";
    private static final String TEST_SCHEMA_RESOURCE = "/database/test-schema.sql";

    @BeforeEach
    void setUpDatabase(TestInfo testInfo) throws Exception {
        System.out.println("Running integration test: " + testInfo.getDisplayName());

        String dbUrl = getConfiguredDatabaseUrl();
        Assumptions.assumeTrue(
            dbUrl != null && !dbUrl.isBlank(),
            "DB integration tests require SENTENCE_BUILDER_DB_URL to point at a dedicated test database "
                + "(see README). Example: jdbc:mysql://localhost:3306/sentence_builder_test?..."
        );

        try (Connection conn = WordDb.openConnection();
             Reader schemaReader = openSchemaReader()) {
            resetSchema(conn, schemaReader);
        }
    }

    protected Connection openConnection() throws SQLException {
        return WordDb.openConnection();
    }

    protected Path writeInputFile(String filePrefix, String content) throws IOException {
        Path inputFile = Files.createTempFile(filePrefix, ".txt");
        Files.writeString(inputFile, content, StandardCharsets.UTF_8);
        return inputFile;
    }

    protected int queryForInt(String sql, Object... params) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Expected one row for integer query: " + sql);
                }
                return rs.getInt(1);
            }
        }
    }

    protected boolean queryForBoolean(String sql, Object... params) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Expected one row for boolean query: " + sql);
                }
                return rs.getBoolean(1);
            }
        }
    }

    private static Reader openSchemaReader() {
        return new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(
                DatabaseIntegrationTestSupport.class.getResourceAsStream(TEST_SCHEMA_RESOURCE),
                "Missing test schema resource: " + TEST_SCHEMA_RESOURCE
            ),
            StandardCharsets.UTF_8
        ));
    }

    private static void bindParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static String getConfiguredDatabaseUrl() {
        String sys = System.getProperty(DB_URL_PROPERTY);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        return System.getenv(DB_URL_PROPERTY);
    }

    private static void resetSchema(Connection conn, Reader schemaReader) throws IOException, SQLException {
        String script = readScript(schemaReader);
        for (String statement : script.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(trimmed)) {
                    ps.execute();
                }
            }
        }
    }

    private static String readScript(Reader schemaReader) throws IOException {
        StringBuilder script = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(schemaReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) {
                    continue;
                }
                script.append(line).append('\n');
            }
        }
        return script.toString();
    }
}
// End of Code by Archisha Sasson
