 -- Code created by Omesh Sana

CREATE DATABASE sentence_builder;
USE sentence_builder;

-- Code created by Luke
DROP TABLE IF EXISTS word_follows;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS files;

-- Code created by Luke
CREATE TABLE words (
    word_id        INT 				AUTO_INCREMENT 	PRIMARY KEY,
    word_text      VARCHAR(100) 	NOT NULL 		UNIQUE,
    total_count    INT 				NOT NULL 		DEFAULT 0,
    start_count    INT 				NOT NULL 		DEFAULT 0,
    end_count      INT 				NOT NULL 		DEFAULT 0,
    is_stopword    BOOLEAN 			NOT NULL 		DEFAULT FALSE,
    last_seen_at   DATETIME 		NULL
);

-- Code created by Luke
CREATE TABLE files (
    file_id        INT 				AUTO_INCREMENT 	PRIMARY KEY,
    file_name      VARCHAR(255) 	NOT NULL,
    file_path      VARCHAR(500) 	NULL,
    imported_at    DATETIME 		NOT NULL 		DEFAULT CURRENT_TIMESTAMP,
    word_count     INT 				NOT NULL 		DEFAULT 0,
    sentence_count INT 				NOT NULL 		DEFAULT 0,

    UNIQUE (file_name, file_path)
);

 -- Code created by Omesh Sana
CREATE TABLE word_file_stats (
    word_id        INT 				NOT NULL,
    file_id        INT 				NOT NULL,
    count_in_file  INT 				NOT NULL 		DEFAULT 0,
    start_in_file  INT 				NOT NULL 		DEFAULT 0,
    end_in_file    INT 				NOT NULL 		DEFAULT 0,
    
    PRIMARY KEY (word_id, file_id),
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE
);

-- Code created by Luke
CREATE TABLE next_word (
    from_word_id     		INT 			NOT NULL,
    to_word_id       		INT 			NOT NULL,
    transition_count 		INT 			NOT NULL 		DEFAULT 0,
    follows_sentence_start 	BOOLEAN 		NOT NULL 		DEFAULT FALSE,
    precedes_sentence_end  	BOOLEAN 		NOT NULL 		DEFAULT FALSE,
    
    PRIMARY KEY (from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES words(word_id)
        ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES words(word_id)
        ON DELETE CASCADE
);

 -- Code created by Omesh Sana
CREATE TABLE generated_sentences (
    sentence_id     	INT 			AUTO_INCREMENT 	PRIMARY KEY,
    sentence_text   	TEXT 			NOT NULL,
    created_at      	DATETIME 		NOT NULL 		DEFAULT CURRENT_TIMESTAMP,
    algorithm_name  	VARCHAR(100) 	NULL,
    starting_word_id 	INT 			NULL,
    
    UNIQUE (sentence_text(512)),
   FOREIGN KEY (starting_word_id) REFERENCES words(word_id)
        ON DELETE SET NULL
);

 -- Code created by Omesh Sana
CREATE TABLE user_input_words (
    user_word_id  		INT 			AUTO_INCREMENT 	PRIMARY KEY,
    word_id       		INT				NOT NULL,
    first_seen_at 		DATETIME 		NOT NULL 		DEFAULT CURRENT_TIMESTAMP,
    last_seen_at  		DATETIME 		NOT NULL 		DEFAULT CURRENT_TIMESTAMP,
    source        		VARCHAR(50) 	NOT NULL 		DEFAULT 'user_input',
   
   FOREIGN KEY (word_id) REFERENCES words(word_id)
        ON DELETE CASCADE
);

