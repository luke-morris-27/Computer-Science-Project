package integration;

import java.nio.file.Path;
import java.sql.Connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import db.FileDao;
import db.WordFileStatsDao;
import parser.FileStatsPersistenceService;
import parser.Normalizer;
import parser.ParseResult;
import parser.TextParser;
import parser.Tokenizer;
import support.DatabaseIntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: FileStatsPersistenceIT
 * Created by: Archisha Sasson
 * Description: Verifies end-to-end persistence of file and word statistics into the database.
 */
// Code by Shriram Janardhan
@Tag("integration")
@Tag("task1-person2")
@DisplayName("File Stats Persistence Integration Tests")
class FileStatsPersistenceIT extends DatabaseIntegrationTestSupport {
    @Test
    @DisplayName("Persist writes files row and per-word file stats")
    void persistWritesFileAndWordFileStats() throws Exception {
        Path inputFile = writeInputFile("file-stats", "Alpha beta. Alpha alpha.");
        TextParser parser = new TextParser(new Tokenizer(), new Normalizer());
        ParseResult result = parser.parse(inputFile);

        try (Connection conn = openConnection()) {
            FileStatsPersistenceService service = new FileStatsPersistenceService(
                new FileDao(conn),
                new WordFileStatsDao(conn)
            );

            long fileId = service.persist(inputFile, result, conn);
            assertTrue(fileId > 0);

            assertEquals(1, queryForInt("SELECT COUNT(*) FROM files WHERE file_id = ?", fileId));
            assertEquals(2, queryForInt("SELECT COUNT(*) FROM word_file_stats WHERE file_id = ?", fileId));
            assertEquals(
                3,
                queryForInt(
                    "SELECT wfs.count_in_file FROM word_file_stats wfs JOIN words w ON w.word_id = wfs.word_id " +
                        "WHERE wfs.file_id = ? AND w.word_text = ?",
                    fileId,
                    "alpha"
                )
            );
        }
    }

    @Test
    @DisplayName("Persist upserts same file metadata instead of duplicating file rows")
    void persistUpsertsExistingFileRow() throws Exception {
        Path inputFile = writeInputFile("file-stats-upsert", "One two.");

        ParseResult first = new ParseResult();
        first.setFileName(inputFile.getFileName().toString());
        first.setTotalWords(2);
        first.setTotalSentences(1);
        first.incrementWordCount("one");
        first.incrementWordCount("two");

        ParseResult second = new ParseResult();
        second.setFileName(inputFile.getFileName().toString());
        second.setTotalWords(4);
        second.setTotalSentences(2);
        second.incrementWordCount("one");
        second.incrementWordCount("two");
        second.incrementWordCount("three");
        second.incrementWordCount("four");

        try (Connection conn = openConnection()) {
            FileStatsPersistenceService service = new FileStatsPersistenceService(
                new FileDao(conn),
                new WordFileStatsDao(conn)
            );

            long firstFileId = service.persist(inputFile, first, conn);
            long secondFileId = service.persist(inputFile, second, conn);

            assertEquals(firstFileId, secondFileId);
            assertEquals(1, queryForInt("SELECT COUNT(*) FROM files"));
            assertEquals(4, queryForInt("SELECT word_count FROM files WHERE file_id = ?", firstFileId));
            assertEquals(2, queryForInt("SELECT sentence_count FROM files WHERE file_id = ?", firstFileId));
        }
    }
}
// End of code by Shriram Janardhan (FileStatsPersistenceIT)
