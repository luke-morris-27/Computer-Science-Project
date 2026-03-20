/*
 * Class: test-schema.sql
 * Created by: Archisha Sasson
 * Description: Defines the test database schema used by ParserDB and generator
 * integration tests, including tables, constraints, and indexes.
 * Example: Resets the schema before integration tests run against a test DB.
 */

-- Code by Archisha Sasson
DROP TABLE IF EXISTS user_input_words;
DROP TABLE IF EXISTS generated_sentences;
DROP TABLE IF EXISTS word_file_stats;
DROP TABLE IF EXISTS next_word;
DROP TABLE IF EXISTS files;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS imports;

CREATE TABLE imports (
    import_id     INT AUTO_INCREMENT PRIMARY KEY,
    filename      VARCHAR(255) NOT NULL,
    word_count    INT NOT NULL DEFAULT 0,
    imported_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_hash     CHAR(64) NOT NULL,
    CONSTRAINT uq_imports_file_hash UNIQUE (file_hash)
);

CREATE TABLE words (
    word_id        INT AUTO_INCREMENT PRIMARY KEY,
    word_text      VARCHAR(100) NOT NULL,
    total_count    INT NOT NULL DEFAULT 0,
    start_count    INT NOT NULL DEFAULT 0,
    end_count      INT NOT NULL DEFAULT 0,
    is_stopword    BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at   TIMESTAMP NULL,
    CONSTRAINT uq_words_word_text UNIQUE (word_text)
);

CREATE TABLE files (
    file_id        INT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(255) NOT NULL,
    file_path      VARCHAR(500) NULL,
    imported_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    word_count     INT NOT NULL DEFAULT 0,
    sentence_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_files_name_path UNIQUE (file_name, file_path)
);

CREATE TABLE word_file_stats (
    word_id        INT NOT NULL,
    file_id        INT NOT NULL,
    count_in_file  INT NOT NULL DEFAULT 0,
    start_in_file  INT NOT NULL DEFAULT 0,
    end_in_file    INT NOT NULL DEFAULT 0,
    PRIMARY KEY (word_id, file_id),
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE
);

CREATE TABLE next_word (
    from_word_id            INT NOT NULL,
    to_word_id              INT NOT NULL,
    transition_count        INT NOT NULL DEFAULT 0,
    follows_sentence_start  BOOLEAN NOT NULL DEFAULT FALSE,
    precedes_sentence_end   BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES words(word_id) ON DELETE CASCADE
);

CREATE TABLE generated_sentences (
    sentence_id       INT AUTO_INCREMENT PRIMARY KEY,
    sentence_text     VARCHAR(2048) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    algorithm_name    VARCHAR(100) NULL,
    starting_word_id  INT NULL,
    FOREIGN KEY (starting_word_id) REFERENCES words(word_id) ON DELETE SET NULL
);

CREATE TABLE user_input_words (
    user_word_id   INT AUTO_INCREMENT PRIMARY KEY,
    word_id        INT NOT NULL,
    first_seen_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source         VARCHAR(50) NOT NULL DEFAULT 'user_input',
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE
);

CREATE INDEX idx_next_word_from ON next_word (from_word_id);
CREATE INDEX idx_next_word_to ON next_word (to_word_id);
CREATE INDEX idx_words_text ON words (word_text);
-- End of Code by Archisha Sasson
