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

## Test Structure
- Unit tests run with `mvn test`
- Database integration tests run with `mvn verify -Pdb-tests`
- Unit tests are tagged with `unit`
- Database integration tests are tagged with `integration`
- ParserDB tests are tagged with `parserdb`
- Generator tests are tagged with `generator`
- DB integration tests are skipped unless `SENTENCE_BUILDER_DB_URL` is set to a dedicated test database
- If your test DB does not use the default MySQL credentials, also set `SENTENCE_BUILDER_DB_USER` and `SENTENCE_BUILDER_DB_PASSWORD`

## Selective Test Commands

### Unit Test Commands
- `mvn test`
  Runs every enabled unit test in `src/test/java` that is handled by the normal Surefire test phase.
- `mvn test -Dgroups=unit`
  Runs only tests tagged with `unit`.
- `mvn test -Dgroups=parserdb`
  Runs enabled ParserDB unit tests such as parser and normalizer tests.
- `mvn test -Dgroups=generator`
  Runs enabled generator unit tests. 
- `mvn test -Dtest=ParserTest`
  Runs one specific unit test class.
- `mvn test -Dtest=NormalizerTest`
  Runs only the normalizer unit test class.
- `mvn test -Dtest=ParserTest#simpleCase`
  Runs one specific unit test method from one class.
- `mvn test -Dtest=ParserTest#edgeCase`
  Runs only the punctuation-heavy parser scenario.
- `mvn test -Dtest=ParserTest#paragraphCase`
  Runs only the paragraph-counting parser scenario.

### Integration Test Commands
- `mvn verify -Pdb-tests`
  Runs the full Maven lifecycle plus the `db-tests` profile, which includes the integration-test phase for `*IT.java` classes.
- `mvn verify -Pdb-tests -Dgroups=integration`
  Runs only tests tagged with `integration`.
- `mvn verify -Pdb-tests -Dgroups=parserdb`
  Runs ParserDB integration tests such as word storage, transition storage, and import tracking scaffolds.
- `mvn verify -Pdb-tests -Dgroups=generator`
  Runs generator integration tests. Right now the weighted generator scaffold is disabled, so this usually reports skipped tests until that code exists.
- `mvn verify -Pdb-tests -Dtest=WordStorageIT`
  Runs one ParserDB integration test class.
- `mvn verify -Pdb-tests -Dtest=TransitionStorageIT`
  Runs only transition-storage integration tests.
- `mvn verify -Pdb-tests -Dtest=ImportTrackingIT`
  Runs only the import-tracking integration test class. This is currently disabled until file hashing and duplicate-import prevention are implemented.
- `mvn verify -Pdb-tests -Dtest=WeightedGeneratorIT`
  Runs only the weighted generator integration test scaffold.
- `mvn verify -Pdb-tests -Dtest=WordStorageIT#repeatedOccurrencesReuseTheSameWordRowAndWordId`
  Runs one specific integration test method.
- `mvn verify -Pdb-tests -Dtest=TransitionStorageIT#repeatedWordPairsIncrementTransitionFrequency`
  Runs only the repeated-transition-frequency integration scenario.
- `mvn verify -Pdb-tests -Dtest=TransitionStorageIT#sentenceBoundariesUpdateStartEndCountsAndBoundaryFlags`
  Runs only the sentence-boundary integration scenario.

### Full Suite Commands
- `mvn clean test`
  Deletes the `target` folder, recompiles, and runs all enabled unit tests.
- `mvn clean verify -Pdb-tests`
  Deletes the `target` folder, recompiles, runs enabled unit tests, packages the project, and runs enabled integration tests from the `db-tests` profile.

### Database Environment Examples
- `SENTENCE_BUILDER_DB_URL=jdbc:mysql://localhost:3306/sentence_builder_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC mvn verify -Pdb-tests -Dgroups=parserdb`
  Uses a dedicated MySQL test database with the default username/password behavior from `WordDb`.
- `SENTENCE_BUILDER_DB_URL=jdbc:mysql://localhost:3306/sentence_builder_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC SENTENCE_BUILDER_DB_USER=root SENTENCE_BUILDER_DB_PASSWORD=your_password mvn verify -Pdb-tests -Dgroups=parserdb`
  Uses a dedicated MySQL test database with explicit credentials.

### Notes
- Unit tests do not require a live database.
- Integration tests require `SENTENCE_BUILDER_DB_URL` to point to a dedicated test database.
- Tests marked with `@Disabled` are discovered by Maven but reported as skipped until the feature is implemented.
- If you run `mvn verify -Pdb-tests`, Maven still runs unit tests first and then runs the integration tests later in the lifecycle.

## Test Output
- Each test uses a descriptive JUnit display name so Maven prints what scenario is running.
- Assertions include failure messages explaining what condition was expected.
- Integration tests print a one-line scenario message before setup begins.

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
