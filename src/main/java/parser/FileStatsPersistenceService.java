/*
 * Class: FileStatsPersistenceService
 * Created by: Person 2
 * Description: Persists file metadata and per-file word counts derived from ParseResult into relational tables.
 * Example: long fileId = service.persist(path, parseResult, connection)
 */
package parser;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import db.FileDao;
import db.WordFileStatsDao;

public class FileStatsPersistenceService {
    private final FileDao fileDao;
    private final WordFileStatsDao wordFileStatsDao;

    public FileStatsPersistenceService(FileDao fileDao, WordFileStatsDao wordFileStatsDao) {
        this.fileDao = fileDao;
        this.wordFileStatsDao = wordFileStatsDao;
    }

    public long persist(Path filePath, ParseResult result, Connection conn) throws SQLException {
        // Guidance:
        // 1. Upsert file row in files table and get file_id.
        // 2. Build per-word aggregates from ParseResult maps.
        // 3. Resolve each word to word_id.
        // 4. Batch upsert rows into word_file_stats.
        // 5. Return file_id.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public static Map<String, WordStatsAggregate> buildWordStats(ParseResult result) {
        // Guidance:
        // Build a map keyed by word text using:
        // - countInFile from wordCounts
        // - startInFile from sentenceStartCounts
        // - endInFile from sentenceEndCounts
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
