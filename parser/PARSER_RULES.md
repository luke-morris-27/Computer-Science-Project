# Parser Rules (Created by Archisha Sasson)

1. Words are normalized to lowercase.
2. Surrounding punctuation is stripped during normalization.
3. Apostrophes and hyphens inside words are preserved (for example: `don't`, `mother-in-law`).
4. Sentence boundaries are detected on `.`, `!`, and `?`.
5. `sentenceStartCounts` tracks the first normalized word after a sentence boundary.
6. `sentenceEndCounts` tracks the last normalized word before a sentence boundary.
7. `nextWordCounts` tracks transitions between consecutive normalized words within the same sentence.
8. Output JSON is written to `parser/output/parse_result.json` by the CLI.
