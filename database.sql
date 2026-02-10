-- Code by Luke
-- Drop existing table (for rerunning code)
DROP TABLE IF EXISTS words;

-- Table: words
CREATE TABLE words (
    -- IDs per row
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- Word
    word VARCHAR(100) NOT NULL UNIQUE,

    -- Number of times the word appears
    total_count INT NOT NULL DEFAULT 0,

    -- Number of times the word appears at the start of a sentence
    sentence_start_count INT NOT NULL DEFAULT 0,

    -- Number of times the word appears at the end of a sentence
    sentence_end_count INT NOT NULL DEFAULT 0
);

-- End of code by Luke
