-- Simple DML script by Omesh Sana

USE sentence_builder;

-- Clear old data
DELETE FROM generated_sentences;
DELETE FROM next_word;
DELETE FROM word_file_stats;
DELETE FROM user_input_words;
DELETE FROM files;
DELETE FROM words;

-- Insert some words
INSERT INTO words (word_text, total_count, start_count, end_count)
VALUES
  ('the', 10, 3, 0),
  ('cat', 5, 1, 0),
  ('sat', 4, 0, 0),
  ('on', 4, 0, 0),
  ('mat', 3, 0, 2),
  ('.', 10, 0, 10);

-- Insert a file record
INSERT INTO files (file_name, file_path, word_count, sentence_count)
VALUES
  ('file.txt', '/tmp/file.txt', 26, 3);

-- Link word stats to that file
INSERT INTO word_file_stats (word_id, file_id, count_in_file, start_in_file, end_in_file)
VALUES
  (1, 1, 10, 3, 0),  -- 'the'
  (2, 1, 5, 1, 0),   -- 'cat'
  (3, 1, 4, 0, 0),   -- 'sat'
  (4, 1, 4, 0, 0),   -- 'on'
  (5, 1, 3, 0, 2),   -- 'mat'
  (6, 1, 10, 0, 10); -- '.'


INSERT INTO next_word (from_word_id, to_word_id, transition_count, follows_sentence_start, precedes_sentence_end)
VALUES
  (1, 2, 3, TRUE,  FALSE), -- the -> cat
  (2, 3, 3, FALSE, FALSE), -- cat -> sat
  (3, 4, 3, FALSE, FALSE), -- sat -> on
  (4, 1, 3, FALSE, FALSE), -- on  -> the
  (1, 5, 2, FALSE, FALSE), -- the -> mat
  (5, 6, 3, FALSE, TRUE);  -- mat -> '.'


INSERT INTO generated_sentences (sentence_text, algorithm_name, starting_word_id)
VALUES
  ('The cat sat on the mat.', 'max_prob', 1),
  ('The cat sat.', 'random', 1);


SELECT * FROM words;
SELECT * FROM files;
SELECT * FROM word_file_stats;
SELECT * FROM next_word;
SELECT * FROM generated_sentences;
