package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import support.DatabaseIntegrationTestSupport;
import ui.WordReportSort;
import ui.WordReportView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@Tag("reporting")
@DisplayName("Database Reporting Integration Tests")
class DbReportingServiceIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Duplicate sentence report returns repeated generated sentences")
    void duplicateSentenceReportReturnsRepeatedGeneratedSentences() throws Exception {
        // sammy 5/2: create one canonical start word so the generated sentence rows can reference a valid id.
        int alphaId = insertWord("alpha");
        // sammy 5/2: insert the same generated sentence twice to model true duplicate history.
        insertGeneratedSentence("alpha beta", "weighted", alphaId);
        // sammy 5/2: insert the same sentence again so the duplicate report has a repeated row to find.
        insertGeneratedSentence("alpha beta", "random", alphaId);
        // sammy 5/2: insert one distinct sentence to prove the duplicates-only filter excludes singletons.
        insertGeneratedSentence("alpha gamma", "greedy", alphaId);

        // sammy 5/2: load the real reporting service against the test database.
        DbReportingService service = new DbReportingService();
        // sammy 5/2: request only duplicate generated sentences from the history report.
        List<String> duplicates = service.listGeneratedSentences(true, 10);

        // sammy 5/2: expect the repeated sentence to be preserved in the duplicate report output.
        assertEquals(List.of("alpha beta"), duplicates);
    }

    @Test
    @DisplayName("Word report includes words that only came from user input")
    void wordReportIncludesUserOnlyWords() throws Exception {
        // sammy 5/2: create one imported-style word row so the mixed report has both imported and user-only examples.
        int importedWordId = insertWord("alpha");
        // sammy 5/2: create one file row to support a normal imported word_file_stats entry.
        long fileId = insertFile("alpha.txt");
        // sammy 5/2: attach imported counts to alpha so the report still covers the original import-based path.
        insertWordFileStat(importedWordId, fileId, 3, 1, 1);
        // sammy 5/2: create a second canonical word that exists only because of user input.
        int userOnlyWordId = insertWord("customword");
        // sammy 5/2: insert a user_input_words row so the custom word is represented by the user-input path.
        insertUserInputWord(userOnlyWordId);

        // sammy 5/2: run the real reporting query that should now include all words from the words table.
        DbReportingService service = new DbReportingService();
        // sammy 5/2: request enough rows to include both the imported word and the user-only word.
        List<WordReportView> rows = service.listWords(WordReportSort.ALPHABETICAL, 10);

        // sammy 5/2: confirm the imported word is still present in the report after the left-join change.
        assertTrue(rows.stream().anyMatch(row -> row.wordText().equals("alpha")));
        // sammy 5/2: confirm the user-only word now appears even without any word_file_stats row.
        assertTrue(rows.stream().anyMatch(row -> row.wordText().equals("customword")));
        // sammy 5/2: verify the user-only word correctly reports zero imported-file counts.
        WordReportView customRow = rows.stream()
            .filter(row -> row.wordText().equals("customword"))
            .findFirst()
            .orElseThrow();
        // sammy 5/2: assert the total imported count stays zero for a word that came only from user input.
        assertEquals(0, customRow.totalCount());
    }

    // sammy 5/2: insert one canonical word row and return its generated key for follow-up relations.
    private int insertWord(String wordText) throws Exception {
        // sammy 5/2: open a direct test connection for the helper insert.
        try (Connection conn = openConnection();
             // sammy 5/2: request generated keys so later helper methods can reference the new word id.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO words (word_text) VALUES (?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            // sammy 5/2: store the caller-provided normalized test word text.
            ps.setString(1, wordText);
            // sammy 5/2: execute the helper insert.
            ps.executeUpdate();
            // sammy 5/2: read back the generated key for downstream inserts.
            try (ResultSet rs = ps.getGeneratedKeys()) {
                // sammy 5/2: move to the generated-key row.
                rs.next();
                // sammy 5/2: return the generated word id to the test method.
                return rs.getInt(1);
            }
        }
    }

    // sammy 5/2: insert one generated sentence history row for duplicate-report testing.
    private void insertGeneratedSentence(String sentenceText, String algorithmName, Integer startingWordId) throws Exception {
        // sammy 5/2: open a direct test connection for the sentence-history insert.
        try (Connection conn = openConnection();
             // sammy 5/2: insert one generated sentence event exactly as the production repository would.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO generated_sentences (sentence_text, algorithm_name, starting_word_id) VALUES (?, ?, ?)"
             )) {
            // sammy 5/2: store the literal generated sentence text.
            ps.setString(1, sentenceText);
            // sammy 5/2: store which algorithm label produced the sentence.
            ps.setString(2, algorithmName);
            // sammy 5/2: bind the nullable starting word id for this history row.
            if (startingWordId == null) {
                // sammy 5/2: use SQL null when there is no valid word id reference.
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                // sammy 5/2: store the supplied canonical word id reference.
                ps.setInt(3, startingWordId);
            }
            // sammy 5/2: execute the test insert.
            ps.executeUpdate();
        }
    }

    // sammy 5/2: insert one file row so imported statistics can reference a valid file id.
    private long insertFile(String fileName) throws Exception {
        // sammy 5/2: open a direct test connection for the file insert helper.
        try (Connection conn = openConnection();
             // sammy 5/2: request generated keys so the test can attach word_file_stats rows.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO files (file_name, file_path, word_count, sentence_count) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            // sammy 5/2: store the file name label for the test row.
            ps.setString(1, fileName);
            // sammy 5/2: use a simple synthetic file path because the schema requires one unique pair.
            ps.setString(2, "/tmp/" + fileName);
            // sammy 5/2: give the file row a small nonzero word count for realism.
            ps.setInt(3, 3);
            // sammy 5/2: give the file row one sentence for realism.
            ps.setInt(4, 1);
            // sammy 5/2: execute the helper insert.
            ps.executeUpdate();
            // sammy 5/2: read back the generated file id.
            try (ResultSet rs = ps.getGeneratedKeys()) {
                // sammy 5/2: move to the generated-key row.
                rs.next();
                // sammy 5/2: return the generated file id for downstream word_file_stats rows.
                return rs.getLong(1);
            }
        }
    }

    // sammy 5/2: insert one imported-file statistics row so the report still covers imported data.
    private void insertWordFileStat(int wordId, long fileId, int countInFile, int startInFile, int endInFile) throws Exception {
        // sammy 5/2: open a direct test connection for the word_file_stats helper insert.
        try (Connection conn = openConnection();
             // sammy 5/2: insert one explicit statistics row for the imported-word scenario.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO word_file_stats (word_id, file_id, count_in_file, start_in_file, end_in_file) VALUES (?, ?, ?, ?, ?)"
             )) {
            // sammy 5/2: bind the canonical word id.
            ps.setInt(1, wordId);
            // sammy 5/2: bind the owning file id.
            ps.setLong(2, fileId);
            // sammy 5/2: bind the total count in that file.
            ps.setInt(3, countInFile);
            // sammy 5/2: bind the sentence-start count in that file.
            ps.setInt(4, startInFile);
            // sammy 5/2: bind the sentence-end count in that file.
            ps.setInt(5, endInFile);
            // sammy 5/2: execute the helper insert.
            ps.executeUpdate();
        }
    }

    // sammy 5/2: insert one user_input_words row so the word is represented by the user-input path.
    private void insertUserInputWord(int wordId) throws Exception {
        // sammy 5/2: open a direct test connection for the user-input helper insert.
        try (Connection conn = openConnection();
             // sammy 5/2: insert one user-input event row tied back to the canonical word id.
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO user_input_words (word_id) VALUES (?)"
             )) {
            // sammy 5/2: bind the canonical word id.
            ps.setInt(1, wordId);
            // sammy 5/2: execute the helper insert.
            ps.executeUpdate();
        }
    }
}
