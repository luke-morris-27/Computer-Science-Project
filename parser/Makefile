# File Created by Archisha Sasson
# Code by Archisha Sasson

BIN_DIR := bin
SRC_DIR := src/parser
TEST_DIR := test/parser
ALT_SRC_DIR := src/main/java/parser
ALT_TEST_DIR := src/test/java/parser
MAIN_CLASS := parser.Main
TEST_CLASS := parser.ParserTestRunner
SAMPLE_FILE := sample_texts/simple.txt
# Test case for parser test runner: simple or edge.
TEST_CASE := simple
SRC_FILES := $(wildcard $(SRC_DIR)/*.java) $(wildcard $(ALT_SRC_DIR)/*.java)
TEST_FILES := $(wildcard $(TEST_DIR)/*.java) $(wildcard $(ALT_TEST_DIR)/*.java)

# Mark targets as phony so they always run when requested.
.PHONY: compile run test test-edge clean

# Compile all source and test Java files into BIN_DIR.
compile:
	@mkdir -p $(BIN_DIR)
	javac -d $(BIN_DIR) $(SRC_FILES) $(TEST_FILES)

# Build first, then run the parser against the default sample file.
run: compile
	java -cp $(BIN_DIR) $(MAIN_CLASS) $(SAMPLE_FILE)

# Build first, then run the manual test runner.
test: compile
	# Regenerate output JSON so it matches the tested input file.
	java -cp $(BIN_DIR) $(MAIN_CLASS) sample_texts/$(if $(filter edge,$(TEST_CASE)),edge_cases,simple).txt
	# Run assertions for the selected test case.
	java -cp $(BIN_DIR) $(TEST_CLASS) $(TEST_CASE)

# Build first, then run explicit edge-case assertions.
test-edge: compile
	# Regenerate output JSON from edge-case input.
	java -cp $(BIN_DIR) $(MAIN_CLASS) sample_texts/edge_cases.txt
	# Run edge-case assertions.
	java -cp $(BIN_DIR) $(TEST_CLASS) edge

# Remove compiled classes by deleting BIN_DIR.
clean:
	rm -rf $(BIN_DIR)
	# Clear JSON output on clean.
	@mkdir -p output
	: > output/parse_result.json

# End of Code by Archisha Sasson
