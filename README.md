# Sentence Builder - CS4485 Senior Design

Spring 2026 Project

## Team Members
- Archisha Sasson
- Luke Morris
- Sammy Pandey
- Omesh Sana
- Shriram Janardhan

## Technologies
- Java 17
- MySQL 8.x
- Maven

## Project Structure
```
pom.xml
src/
├── main/java/parser/              - Parser implementation
├── test/java/parser/              - JUnit 5 parser tests
└── test/resources/parser/         - Parser sample input files
database/
└── SentenceBuilderDatabase.sql    - Database schema
PARSER_RULES.md                    - Parser behavior notes
```

## Setup
1. Install JDK 17
2. Install MySQL
3. Run `database/SentenceBuilderDatabase.sql`
4. Open in IntelliJ
5. Maven → Reload Project
6. Run parser with `parser.Main` (input file path required)

## Build and Test
- `mvn clean test`

## Parser Tests
- Test class: `src/test/java/parser/ParserTest.java`
- Test fixtures: `src/test/resources/parser/`
- Fixture loading: tests load files from classpath resources, so they run consistently in IDE and Maven.

### Test Cases
- `simpleCase` uses `simple.txt` and verifies baseline parsing counts:
  words, sentences, paragraph count, and key start/end/next-word map entries.
- `edgeCase` uses `edge_cases.txt` and verifies punctuation-heavy text handling:
  apostrophes (`don't`), hyphens (`mother-in-law`), and sentence boundaries.
- `paragraphCase` uses `paragraphs.txt` and verifies paragraph detection:
  a blank line separates two paragraphs and expected totals are asserted.

## Parser Output
- CLI JSON output is written to `target/parse_result.json`.
