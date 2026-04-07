package ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import generator.AutocompleteService;
import generator.GenerationAlgorithm;
import generator.GenerationService;
import generator.WeightedWord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Class: UiFlowSmokeTest
 * Created by: Archisha Sasson
 * Description: Verifies the end-to-end controller flow across import, generation, autocomplete, and reporting.
 */
@Tag("unit")
@Tag("task2-person5")
@DisplayName("UI Flow Smoke Test")
class UiFlowSmokeTest {
    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("Running unit test: " + testInfo.getDisplayName() + " | Verifies end-to-end UI controller smoke flow.");
    }

    @Test
    @DisplayName("Import -> Generate -> Autocomplete -> Reports flow")
    void uiFlowSmoke() throws Exception {
        ImportController importController = new ImportController();
        GenerationService generationService = new GenerationService(
            (start, max) -> "weighted sentence",
            (start, max) -> "greedy sentence",
            (start, max) -> "random sentence"
        );
        GenerateController generateController = new GenerateController(generationService);

        AutocompleteService autocompleteService = new AutocompleteService(new FakeAutocompleteGateway());
        AutocompleteController autocompleteController = new AutocompleteController(autocompleteService);

        ReportsController reportsController = new ReportsController(new FakeReportingService());

        Path file = Files.createTempFile("ui-flow", ".txt");
        Files.writeString(file, "hello world");

        ImportViewState importState = importController.validatePath(file.toString());
        assertTrue(importState.valid());

        GenerateViewState generateState = generateController.generate(GenerationAlgorithm.GREEDY, "hello", 5);
        assertTrue(generateState.success());

        AutocompleteViewState autocompleteState = autocompleteController.onWordCommitted("hello", ' ', 3);
        assertEquals(AutocompleteViewState.AutocompleteOutcome.SHOW_RESULTS, autocompleteState.outcome());
        assertTrue(autocompleteState.suggestionsRequested());
        assertTrue(autocompleteState.hasSuggestions());
        assertEquals(List.of("world"), autocompleteState.suggestions());
        assertEquals("Loaded 1 suggestions after 'hello'.", autocompleteState.feedbackMessage());

        List<WordReportView> report = reportsController.listWords(WordReportSort.ALPHABETICAL, 10);
        assertEquals(1, report.size());
        assertEquals("hello", report.get(0).wordText());
    }

    private static final class FakeAutocompleteGateway implements generator.AutocompleteGateway {
        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) {
            return List.of(new WeightedWord(2, "world", 3));
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            // no-op
        }
    }

    private static final class FakeReportingService implements UiReportingService {
        @Override
        public List<WordReportView> listWords(WordReportSort sort, int limit) throws SQLException {
            return List.of(new WordReportView("hello", 5, 2, 1));
        }

        @Override
        public List<WordReportView> listWords(WordReportSort sort, int limit, String searchText) throws SQLException {
            return List.of(new WordReportView("hello", 5, 2, 1));
        }

        @Override
        public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) throws SQLException {
            return List.of("hello world");
        }
    }
}
