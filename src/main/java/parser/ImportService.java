package parser;

import db.FileDao;
import db.ImportDao;
import db.NextWordDao;
import db.WordCountsDao;
import db.WordFileStatsDao;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs an atomic import: words/start+end counts/transitions + import/file metadata + word_file_stats
 * are written in a single JDBC transaction (all-or-nothing).
 */
public final class ImportService {
    private final TextParser parser;

    public ImportService(TextParser parser) {
        this.parser = parser;
    }

    public ParseResult importFile(Path path, String fileHash) throws SQLException, java.io.IOException {
        ImportParseResult parsed = parser.parseForImport(path);
        try (Connection conn = WordDb.openConnection()) {
            conn.setAutoCommit(false);
            try {
                persistAll(conn, path, parsed, fileHash);
                conn.commit();
                return parsed.parseResult();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void persistAll(Connection conn, Path path, ImportParseResult parsed, String fileHash) throws SQLException {
        ParseResult result = parsed.parseResult();

        new ImportDao(conn).insertImport(
            path.getFileName().toString(),
            result.getTotalWords(),
            result.getImportedAt(),
            fileHash
        );

        new FileStatsPersistenceService(new FileDao(conn), new WordFileStatsDao(conn))
            .persist(path, result, conn);

        // Cache word text -> word_id to minimize repeated lookups/inserts.
        Map<String, Integer> wordIds = new HashMap<>();
        WordCountsDao countsDao = new WordCountsDao(conn);
        NextWordDao nextWordDao = new NextWordDao(conn);

        for (Map.Entry<String, Integer> entry : result.getSentenceStartCounts().entrySet()) {
            String word = entry.getKey();
            Integer value = entry.getValue();
            int count = value == null ? 0 : value.intValue();
            int wordId = getOrCreateCached(wordIds, word, conn);
            countsDao.incStartBy(wordId, count);
        }

        for (Map.Entry<String, Integer> entry : result.getSentenceEndCounts().entrySet()) {
            String word = entry.getKey();
            Integer value = entry.getValue();
            int count = value == null ? 0 : value.intValue();
            int wordId = getOrCreateCached(wordIds, word, conn);
            countsDao.incEndBy(wordId, count);
        }

        for (Map.Entry<TransitionKey, TransitionStats> entry : parsed.transitions().entrySet()) {
            TransitionKey key = entry.getKey();
            TransitionStats stats = entry.getValue();
            if (stats == null || stats.count() <= 0) {
                continue;
            }
            int fromId = getOrCreateCached(wordIds, key.fromWord(), conn);
            int toId = getOrCreateCached(wordIds, key.toWord(), conn);
            nextWordDao.incrementBy(
                fromId,
                toId,
                stats.count(),
                stats.followsSentenceStart(),
                stats.precedesSentenceEnd()
            );
        }
    }

    private static int getOrCreateCached(Map<String, Integer> cache, String word, Connection conn) throws SQLException {
        String safe = word == null ? "" : word;
        Integer existing = cache.get(safe);
        if (existing != null) {
            return existing;
        }
        int id = WordDb.getOrCreateWordId(safe, conn);
        cache.put(safe, id);
        return id;
    }
}

