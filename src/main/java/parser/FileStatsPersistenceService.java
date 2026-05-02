/*
 * Class: FileStatsPersistenceService
 * Created by: Shriram Janardhan
 * Description: Persists file metadata and per-file word counts derived from ParseResult into relational tables.
 * Example: long fileId = service.persist(path, parseResult, connection)
 */
package parser;

import db.FileDao;
import db.WordFileStatInput;
import db.WordFileStatsDao;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

// Code by Shriram Janardhan
public class FileStatsPersistenceService {
    private final FileDao fileDao;
    private final WordFileStatsDao wordFileStatsDao;

    public FileStatsPersistenceService(FileDao fileDao, WordFileStatsDao wordFileStatsDao) {
        this.fileDao = fileDao;
        this.wordFileStatsDao = wordFileStatsDao;
    }

    public long persist(Path filePath, ParseResult result, Connection conn) throws SQLException {

        String fileName = filePath.getFileName().toString();

        // 1. Upsert file row
        long fileId = fileDao.upsertFile(
                fileName,
                filePath.toString(),
                result.getTotalWords(),
                result.getTotalSentences()
        );

        // 2. Build word statistics
        Map<String, WordStatsAggregate> stats = buildWordStats(result);

        // 3. Resolve words to word_ids - getOrCreate ensures words exist even if parser skipped DB writes
        List<WordFileStatInput> rows = new ArrayList<>();

        for (Map.Entry<String, WordStatsAggregate> entry : stats.entrySet()) {

            String word = entry.getKey();
            WordStatsAggregate agg = entry.getValue();

            int wordId = WordDb.getOrCreateWordId(word, conn);

            rows.add(new WordFileStatInput(
                    wordId,
                    agg.countInFile(),
                    agg.startInFile(),
                    agg.endInFile()
            ));
        }

        // 4. Batch upsert
        wordFileStatsDao.upsertStats(fileId, rows);

        // 5. Return file_id
        return fileId;
    }

    public static Map<String, WordStatsAggregate> buildWordStats(ParseResult result) {

        Map<String, WordStatsAggregate> stats = new HashMap<>();

        Map<String, Integer> wordCounts = result.getWordCounts();
        Map<String, Integer> startCounts = result.getSentenceStartCounts();
        Map<String, Integer> endCounts = result.getSentenceEndCounts();

        for (String word : wordCounts.keySet()) {

            int count = wordCounts.getOrDefault(word, 0);
            int start = startCounts.getOrDefault(word, 0);
            int end = endCounts.getOrDefault(word, 0);

            stats.put(word, new WordStatsAggregate(count, start, end));
        }

        return stats;
    }
}
// End of code by Shriram Janardhan (FileStatsPersistenceService - file and word_file_stats persistence)
