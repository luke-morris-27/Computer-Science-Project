/*
 * Class: DbReportingService
 * Description: Loads word and generated-sentence reports from the relational schema
 * (word_file_stats, next_word, generated_sentences) via MySQL/MariaDB.
 */
package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import parser.Normalizer;
import parser.WordDb;
import ui.UiReportingService;
import ui.WordReportSort;
import ui.WordReportView;

public final class DbReportingService implements UiReportingService {
    private final Normalizer normalizer = new Normalizer();

    @Override
    public List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException {
        return listWords(sort, limit, "", "");
    }

    @Override
    public List<WordReportView> listWords(WordReportSort sort, int limit, String searchText) throws SQLException {
        return listWords(sort, limit, searchText, "");
    }

    @Override
    public List<WordReportView> listWords(WordReportSort sort, int limit, String searchText, String secondWord)
            throws SQLException {
        List<WordReportView> rows = loadAggregatedWordRows();
        String s1 = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        String s2 = secondWord == null ? "" : secondWord.trim().toLowerCase(Locale.ROOT);

        if (!s1.isBlank()) {
            rows = rows.stream()
                .filter(w -> w.wordText().toLowerCase(Locale.ROOT).equals(s1))
                .toList();
        }

        Comparator<WordReportView> comparator = comparatorFor(sort);
        rows = new ArrayList<>(rows);
        rows.sort(comparator);

        if (s2.isBlank()) {
            return rows.stream()
                .limit(limit <= 0 ? 100 : limit)
                .map(w -> new WordReportView(
                    w.wordText(), w.totalCount(), w.startCount(), w.endCount(), 0, 0))
                .toList();
        }

        String secondNorm = normalizer.normalize(s2);
        List<WordReportView> out = new ArrayList<>();
        int cap = limit <= 0 ? 100 : limit;
        for (WordReportView w : rows) {
            if (out.size() >= cap) {
                break;
            }
            String baseNorm = normalizer.normalize(w.wordText());
            int follows = transitionCount(baseNorm, secondNorm);
            int precedes = transitionCount(secondNorm, baseNorm);
            out.add(new WordReportView(
                w.wordText(), w.totalCount(), w.startCount(), w.endCount(), follows, precedes));
        }
        return out;
    }

    @Override
    public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException {
        int cap = limit <= 0 ? 100 : limit;
        if (!onlyDuplicates) {
            try (Connection conn = WordDb.openConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT sentence_text FROM generated_sentences ORDER BY sentence_id DESC LIMIT ?"
                 )) {
                ps.setInt(1, cap);
                try (ResultSet rs = ps.executeQuery()) {
                    return readStringColumn(rs, "sentence_text");
                }
            }
        }

        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT sentence_text, MAX(sentence_id) AS mid "
                     + "FROM generated_sentences "
                     + "GROUP BY sentence_text "
                     + "HAVING COUNT(*) > 1 "
                     + "ORDER BY mid DESC "
                     + "LIMIT ?"
             )) {
            ps.setInt(1, cap);
            try (ResultSet rs = ps.executeQuery()) {
                return readStringColumn(rs, "sentence_text");
            }
        }
    }

    private List<WordReportView> loadAggregatedWordRows() throws SQLException {
        // sammy 5/2: use a left join so words added outside file imports still appear in the reports list.
        String sql =
            "SELECT w.word_text, "
                + "COALESCE(SUM(wfs.count_in_file), 0) AS total_count, "
                + "COALESCE(SUM(wfs.start_in_file), 0) AS start_count, "
                + "COALESCE(SUM(wfs.end_in_file), 0) AS end_count "
                + "FROM words w "
                + "LEFT JOIN word_file_stats wfs ON wfs.word_id = w.word_id "
                + "GROUP BY w.word_id, w.word_text";

        // sammy 5/2: open one reporting connection for the word aggregation query.
        try (Connection conn = WordDb.openConnection();
             // sammy 5/2: prepare the all-words report query exactly once.
             PreparedStatement ps = conn.prepareStatement(sql);
             // sammy 5/2: execute the query and stream the aggregated rows back.
             ResultSet rs = ps.executeQuery()) {
            // sammy 5/2: collect the report rows into a mutable list before higher-level sorting/filtering.
            List<WordReportView> list = new ArrayList<>();
            // sammy 5/2: convert every result row into the ui-facing report record.
            while (rs.next()) {
                // sammy 5/2: keep words with zero imported-file counts visible by preserving the left-join zeros.
                list.add(new WordReportView(
                    rs.getString("word_text"),
                    rs.getInt("total_count"),
                    rs.getInt("start_count"),
                    rs.getInt("end_count"),
                    0,
                    0
                ));
            }
            // sammy 5/2: return the complete word list so reports can sort and filter it later.
            return list;
        }
    }

    private int transitionCount(String fromWordText, String toWordText) throws SQLException {
        if (fromWordText == null || fromWordText.isBlank() || toWordText == null || toWordText.isBlank()) {
            return 0;
        }
        String sql =
            "SELECT nw.transition_count "
                + "FROM next_word nw "
                + "JOIN words wf ON wf.word_id = nw.from_word_id "
                + "JOIN words wt ON wt.word_id = nw.to_word_id "
                + "WHERE wf.word_text = ? AND wt.word_text = ?";

        try (Connection conn = WordDb.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromWordText);
            ps.setString(2, toWordText);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("transition_count");
                }
                return 0;
            }
        }
    }

    private static Comparator<WordReportView> comparatorFor(WordReportSort sort) {
        WordReportSort effective = sort == null ? WordReportSort.ALPHABETICAL : sort;
        return switch (effective) {
            case TOTAL_COUNT_DESC -> Comparator.comparingInt(WordReportView::totalCount).reversed()
                .thenComparing(WordReportView::wordText);
            case START_COUNT_DESC -> Comparator.comparingInt(WordReportView::startCount).reversed()
                .thenComparing(WordReportView::wordText);
            case END_COUNT_DESC -> Comparator.comparingInt(WordReportView::endCount).reversed()
                .thenComparing(WordReportView::wordText);
            case ALPHABETICAL -> Comparator.comparing(WordReportView::wordText);
        };
    }

    private static List<String> readStringColumn(ResultSet rs, String column) throws SQLException {
        List<String> list = new ArrayList<>();
        while (rs.next()) {
            list.add(rs.getString(column));
        }
        return list;
    }
}
