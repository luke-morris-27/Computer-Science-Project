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

CREATE TABLE word_follows (
    word_id INT NOT NULL,
    follows_word_id INT NOT NULL,

    -- Number of times follows_word appears immediately after word
    follow_count INT NOT NULL DEFAULT 0,

    -- Composite primary key ensures uniqueness
    PRIMARY KEY (word_id, follows_word_id),

    FOREIGN KEY (word_id)
        REFERENCES words(id)
        ON DELETE CASCADE,

    FOREIGN KEY (follows_word_id)
        REFERENCES words(id)
        ON DELETE CASCADE
);

-- End of code by Luke
