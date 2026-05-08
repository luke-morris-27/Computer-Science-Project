# Sentence Builder - CS4485 Senior Design

Spring 2026 Project

## Overview
Sentence Builder is a JavaFX desktop app that imports .txt files, stores parsed word data in MySQL, and uses that data to generate sentence drafts and autocomplete suggestions. The app also includes reports for imported word counts, sentence history, and word relationship statistics.

## Team Members
- Archisha Sasson
- Luke Morris
- Sammy Pandey
- Omesh Sana
- Shriram Janardhan

## Technologies
- Java 17
- Maven
- MySQL 8.x or MariaDB 10.x (server on `localhost:3306` by default)

## How to Run Locally
Before running the app, make sure you have the technologies listed above

### 1. Create the database
Run the schema file in MySQL:
mysql -u root -p < database/SentenceBuilderDatabase.sql

This creates the `sentence_builder` database and all required tables.

### 2. Configure database login
Copy the example environment file:
cp .env.example .env

Open `.env` and set your local MySQL username and password:
SENTENCE_BUILDER_DB_URL=jdbc:mysql://localhost:3306/sentence_builder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SENTENCE_BUILDER_DB_USER=root
SENTENCE_BUILDER_DB_PASSWORD=your_mysql_password

Do not commit .env; it is already ignored by Git.

### 3. Run the app
From the project root, run:
mvn javafx:run

If using IntelliJ, reload the Maven project first, then run `ui.SentenceBuilderApp`.


## Build and Test
- `mvn clean test` — unit tests (tests that open JDBC, e.g. `db.ImportDaoIT`, need a reachable MySQL database matching `DatabaseConfig`).
- `mvn clean verify -Pdb-tests` — runs `*IT.java` integration tests; requires `SENTENCE_BUILDER_DB_URL` pointing at a **dedicated test database** where the schema from `test-schema.sql` can be dropped and recreated each run.

## Test Structure
- Unit tests run with `mvn test`
- Database integration tests run with `mvn verify -Pdb-tests`
- Unit tests are tagged with `unit`
- Database integration tests are tagged with `integration`
- ParserDB tests are tagged with `parserdb`
- Generator tests are tagged with `generator`
- DB integration tests are **skipped** unless `SENTENCE_BUILDER_DB_URL` is set to a dedicated test database (see `DatabaseIntegrationTestSupport`).
- If your test DB does not use the default credentials, also set `SENTENCE_BUILDER_DB_USER` and `SENTENCE_BUILDER_DB_PASSWORD`.

## Selective Test Commands
- `mvn test`
  Runs the main unit test suite.

- `mvn test -Dtest=ParserTest`
  Runs one specific test class.

- `mvn test -Dtest=ParserTest#simpleCase`
  Runs one specific test method.

- `mvn verify -Pdb-tests`
  Runs database integration tests. Use a dedicated test database because these tests reset schema data.

### Notes
- Most unit tests do not require a database. Any test that calls `WordDb.openConnection()` expects MySQL/MariaDB at the configured URL.
- Integration tests (`*IT.java` extending `DatabaseIntegrationTestSupport`) require `SENTENCE_BUILDER_DB_URL` to point to a dedicated test database.
- Tests marked with `@Disabled` are discovered by Maven but reported as skipped until the feature is implemented.
- If you run `mvn verify -Pdb-tests`, Maven still runs unit tests first and then runs the integration tests later in the lifecycle.
