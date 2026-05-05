package parser;

import db.ConnectionTransactionRunner;
import db.FileDao;
import db.ImportDao;
import db.NextWordDao;
import db.WordCountsDao;
import db.WordFileStatsDao;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs an atomic import so parser statistics, file metadata, and transition data
 * all commit together or all roll back together.
 */
public final class ImportService {
    private final TextParser parser;
    private final ConnectionTransactionRunner transactionRunner;

    public ImportService(TextParser parser) {
        this(parser, new ConnectionTransactionRunner(WordDb::openConnection));
    }

    ImportService(TextParser parser, ConnectionTransactionRunner transactionRunner) {
        this.parser = parser;
        this.transactionRunner = transactionRunner;
    }

    public ParseResult importFile(Path path, String fileHash) throws SQLException, IOException {
        ImportParseResult parsed = parser.parseForImport(path);
        return transactionRunner.run(conn -> {
            persistAll(conn, path, parsed, fileHash);
            return parsed.parseResult();
        });
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

        Map<String, Integer> wordIds = new HashMap<>();
        WordCountsDao countsDao = new WordCountsDao(conn);
        NextWordDao nextWordDao = new NextWordDao(conn);

        for (Map.Entry<String, Integer> entry : result.getSentenceStartCounts().entrySet()) {
            int wordId = getOrCreateCached(wordIds, entry.getKey(), conn);
            countsDao.incStartBy(wordId, safeCount(entry.getValue()));
        }

        for (Map.Entry<String, Integer> entry : result.getSentenceEndCounts().entrySet()) {
            int wordId = getOrCreateCached(wordIds, entry.getKey(), conn);
            countsDao.incEndBy(wordId, safeCount(entry.getValue()));
        }

        for (Map.Entry<TransitionKey, TransitionStats> entry : parsed.transitions().entrySet()) {
            TransitionStats stats = entry.getValue();
            if (stats == null || stats.count() <= 0) {
                continue;
            }

            int fromId = getOrCreateCached(wordIds, entry.getKey().fromWord(), conn);
            int toId = getOrCreateCached(wordIds, entry.getKey().toWord(), conn);
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
        Integer existing = cache.get(word);
        if (existing != null) {
            return existing;
        }

        int wordId = WordDb.getOrCreateWordId(word, conn);
        cache.put(word, wordId);
        return wordId;
    }

    private static int safeCount(Integer count) {
        return count == null ? 0 : count.intValue();
    }
}
