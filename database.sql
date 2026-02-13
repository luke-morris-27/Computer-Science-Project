-- Code by Luke
-- Drop existing tables (for rerunning code)
DROP TABLE IF EXISTS word_follows;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS files;

-- Table: files
CREATE TABLE files (
    id INT AUTO_INCREMENT PRIMARY KEY,

    -- File name or path
    filename VARCHAR(255) NOT NULL,

    -- When the file was imported
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Total words found in this file
    total_word_count INT NOT NULL DEFAULT 0
);

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

-- Table: word_follows
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
