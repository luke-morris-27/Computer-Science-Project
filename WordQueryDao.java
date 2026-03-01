public Integer getWordId(String wordText) throws SQLException {
    String sql = "SELECT word_id FROM words WHERE word_text = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, wordText);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("word_id");
            }
            return null;
        }
    }
}

public List<NextWord> getNextWords(int wordId)
