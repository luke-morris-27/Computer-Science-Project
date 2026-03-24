/*
 * Class: SentenceBuilderApp
 * Created by: Archisha Sasson
 * Description: JavaFX preview application that surfaces the current ui package
 * progress through an import-first, draft-driven workflow.
 * Example: mvn javafx:run
 */
package ui;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import generator.AutocompleteGateway;
import generator.AutocompleteService;
import generator.GenerationAlgorithm;
import generator.GenerationService;
import generator.WeightedWord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Application;
import parser.Normalizer;
import parser.ParseResult;
import parser.TextParser;
import parser.Tokenizer;

public class SentenceBuilderApp extends Application {
    private static final DateTimeFormatter IMPORT_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final DemoUiState demoState = new DemoUiState();
    private final Normalizer normalizer = new Normalizer();
    private final TextParser previewParser = new TextParser(new Tokenizer(), new Normalizer(), false);
    private final ImportController importController = new ImportController();
    private final AutocompleteController autocompleteController =
        new AutocompleteController(new AutocompleteService(new InMemoryAutocompleteGateway(demoState)));
    private final GenerateController generateController =
        new GenerateController(new GenerationService(
            (startWord, maxWords) -> demoState.generateSentence(GenerationAlgorithm.WEIGHTED, startWord, maxWords),
            (startWord, maxWords) -> demoState.generateSentence(GenerationAlgorithm.GREEDY, startWord, maxWords)
        ));
    private final ReportsController reportsController =
        new ReportsController(new InMemoryReportingService(demoState));

    private final ObservableList<WordReportView> wordRows = FXCollections.observableArrayList();
    private final ObservableList<String> sentenceRows = FXCollections.observableArrayList();

    private final Label activeFileValue = createValueLabel("No file imported");
    private final Label statsValue = createValueLabel("Words 0 | Sentences 0 | Paragraphs 0");
    private final Label importMessageLabel = createValueLabel("Step 1: import a text file to unlock the rest of the workspace.");
    private final Label draftStatusValue = createValueLabel("Draft is empty");
    private final Label draftHelpValue = createValueLabel("Click autocomplete suggestions to build a sentence here.");
    private final TextArea activityLog = new TextArea();
    private final TextArea sentenceDraftArea = new TextArea();

    private Tab generateTab;
    private Tab autocompleteTab;
    private Tab reportsTab;

    private TextField generateStartWordField;
    private ComboBox<GenerationAlgorithm> algorithmBox;
    private Spinner<Integer> maxWordsSpinner;
    private TextArea generateOutputArea;

    private TextField autocompleteCommittedWordField;
    private Spinner<Integer> suggestionLimitSpinner;
    private ListView<String> suggestionsView;

    private ComboBox<WordReportSort> reportSortBox;
    private Spinner<Integer> reportWordLimitSpinner;
    private Spinner<Integer> reportSentenceLimitSpinner;
    private CheckBox duplicatesOnlyCheckBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        TabPane workspaceTabs = createWorkspaceTabs(stage);

        SplitPane splitPane = new SplitPane(workspaceTabs, createDraftPane());
        splitPane.setDividerPositions(0.7);

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setCenter(splitPane);
        root.setBottom(createActivityLogPane());
        root.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f7efe3, #efe2d1);" +
            "-fx-font-family: 'Georgia';"
        );

        Scene scene = new Scene(root, 1320, 860);
        stage.setTitle("Team 43: Sentence Builder");
        stage.setScene(scene);
        stage.show();

        setWorkspaceEnabled(false);
        refreshDraftMetadata();
        refreshReports();
        log("UI preview started. Import a text file first, then build a draft with generation and autocomplete.");
    }

    private TabPane createWorkspaceTabs(Stage stage) {
        generateTab = createGenerateTab();
        autocompleteTab = createAutocompleteTab();
        reportsTab = createReportsTab();

        TabPane tabPane = new TabPane(
            createImportTab(stage),
            generateTab,
            autocompleteTab,
            reportsTab
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return tabPane;
    }

    private VBox createHeader() {
        Label title = new Label("Team 43: Sentence Builder");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        Label subtitle = new Label("Interactive workspace for import, generation, autocomplete, and reports.");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #8a3a46;");

        VBox summaryCard = new VBox(6,
            new Label("Active Import"),
            activeFileValue,
            statsValue
        );
        summaryCard.setPadding(new Insets(14));
        summaryCard.setStyle(
            "-fx-background-color: rgba(255,248,240,0.88);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(111,29,42,0.20);" +
            "-fx-border-radius: 14;"
        );

        VBox heading = new VBox(4, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(20, heading, spacer, summaryCard);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 22, 18, 22));
        return new VBox(topBar);
    }

    private VBox createDraftPane() {
        Label title = titledLabel("Sentence Draft");
        Label instructions = new Label("Type directly or single-click an autocomplete suggestion to append it.");
        instructions.setWrapText(true);

        sentenceDraftArea.setWrapText(true);
        sentenceDraftArea.setPromptText("Your working sentence lives here...");
        sentenceDraftArea.setPrefRowCount(12);
        sentenceDraftArea.textProperty().addListener((obs, oldValue, newValue) -> refreshDraftMetadata());

        Button useLastWordForSuggestionsButton = new Button("Use Last Word for Suggestions");
        useLastWordForSuggestionsButton.setOnAction(event -> {
            String lastWord = getLastDraftWord();
            if (lastWord.isBlank()) {
                log("Draft is empty, so there is no last word to suggest from.");
                return;
            }
                autocompleteCommittedWordField.setText(lastWord);
                requestSuggestions(lastWord, ' ', suggestionLimitSpinner.getValue(), true);
        });

        Button useLastWordForGenerationButton = new Button("Use Last Word for Generation");
        useLastWordForGenerationButton.setOnAction(event -> {
            String lastWord = getLastDraftWord();
            if (lastWord.isBlank()) {
                log("Draft is empty, so there is no last word to generate from.");
                return;
            }
            generateStartWordField.setText(lastWord);
            log("Generation seed updated from draft: " + lastWord);
        });

        Button removeLastWordButton = new Button("Remove Last Word");
        removeLastWordButton.setOnAction(event -> removeLastWordFromDraft());

        Button clearDraftButton = new Button("Clear Draft");
        clearDraftButton.setOnAction(event -> {
            sentenceDraftArea.clear();
            log("Draft cleared.");
        });

        HBox actions = new HBox(10,
            useLastWordForSuggestionsButton,
            useLastWordForGenerationButton,
            removeLastWordButton,
            clearDraftButton
        );

        VBox box = new VBox(14,
            title,
            instructions,
            sentenceDraftArea,
            draftStatusValue,
            draftHelpValue,
            actions
        );
        box.setPadding(new Insets(22));
        box.setStyle(cardStyle("#fff7ee"));
        VBox.setVgrow(sentenceDraftArea, Priority.ALWAYS);
        return box;
    }

    private Tab createImportTab(Stage stage) {
        TextField pathField = new TextField();
        pathField.setPromptText("Select a .txt file to validate and preview");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Text File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.*"));
            java.io.File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
            }
        });

        Button loadButton = new Button("Validate and Load");
        loadButton.setOnAction(event -> handleImport(pathField.getText()));

        VBox steps = new VBox(8,
            titledLabel("Import Preview"),
            new Label("1. Choose a text file."),
            new Label("2. Load it into the in-memory preview."),
            new Label("3. Use Generate and Autocomplete to build sentences from that imported data."),
            new HBox(10, pathField, browseButton, loadButton),
            importMessageLabel
        );
        steps.setPadding(new Insets(22));
        steps.setStyle(cardStyle("#fdf4ea"));

        Tab tab = new Tab("Import", steps);
        tab.setClosable(false);
        return tab;
    }

    private Tab createGenerateTab() {
        algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll(GenerationAlgorithm.WEIGHTED, GenerationAlgorithm.GREEDY);
        algorithmBox.setValue(GenerationAlgorithm.WEIGHTED);

        generateStartWordField = new TextField();
        generateStartWordField.setPromptText("Optional start word. Leave blank to use the draft's last word.");

        maxWordsSpinner = new Spinner<>();
        maxWordsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10));
        maxWordsSpinner.setEditable(true);

        generateOutputArea = new TextArea();
        generateOutputArea.setEditable(false);
        generateOutputArea.setWrapText(true);
        generateOutputArea.setPromptText("Generated output appears here");
        generateOutputArea.setPrefRowCount(8);

        Button generateButton = new Button("Generate / Continue");
        generateButton.setOnAction(event -> handleGenerate());

        Button replaceDraftButton = new Button("Replace Draft with Output");
        replaceDraftButton.setOnAction(event -> {
            if (generateOutputArea.getText().isBlank()) {
                log("There is no generated output to copy into the draft.");
                return;
            }
            sentenceDraftArea.setText(generateOutputArea.getText().trim());
            log("Draft replaced with generated output.");
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Algorithm"), 0, 0);
        form.add(algorithmBox, 1, 0);
        form.add(new Label("Start word"), 0, 1);
        form.add(generateStartWordField, 1, 1);
        form.add(new Label("Max words"), 0, 2);
        form.add(maxWordsSpinner, 1, 2);

        HBox buttonRow = new HBox(10, generateButton, replaceDraftButton);

        VBox content = new VBox(18,
            titledLabel("Sentence Generation"),
            new Label("Leave the start word blank and the app will continue from the last word in your draft when possible."),
            form,
            buttonRow,
            generateOutputArea
        );
        content.setPadding(new Insets(22));
        content.setStyle(cardStyle("#f9eee6"));

        Tab tab = new Tab("Generate", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createAutocompleteTab() {
        autocompleteCommittedWordField = new TextField();
        autocompleteCommittedWordField.setPromptText("Committed word. Leave blank to use the draft's last word.");

        suggestionLimitSpinner = new Spinner<>();
        suggestionLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        suggestionLimitSpinner.setEditable(true);

        suggestionsView = new ListView<>();
        suggestionsView.setPrefHeight(260);
        suggestionsView.setPlaceholder(new Label("Suggestions show up here after you request them."));
        suggestionsView.setOnMouseClicked(event -> {
            String selectedWord = suggestionsView.getSelectionModel().getSelectedItem();
            if (selectedWord != null && !selectedWord.isBlank()) {
                appendWordToDraft(selectedWord);
                autocompleteCommittedWordField.setText(selectedWord);
                requestSuggestions(selectedWord, ' ', suggestionLimitSpinner.getValue(), false);
            }
        });

        Button requestSuggestionsButton = new Button("Get Suggestions");
        requestSuggestionsButton.setOnAction(event -> {
            String committedWord = autocompleteCommittedWordField.getText().isBlank()
                ? getLastDraftWord()
                : autocompleteCommittedWordField.getText();
            if (committedWord.isBlank()) {
                log("There is no word available to request suggestions from yet.");
                return;
            }
            autocompleteCommittedWordField.setText(committedWord);
            requestSuggestions(committedWord, ' ', suggestionLimitSpinner.getValue(), true);
        });

        TextField registerWordField = new TextField();
        registerWordField.setPromptText("Register a new word");
        HBox.setHgrow(registerWordField, Priority.ALWAYS);

        Button registerButton = new Button("Register Word");
        registerButton.setOnAction(event -> {
            try {
                autocompleteController.registerUserWord(registerWordField.getText());
                log("Registered autocomplete word: " + registerWordField.getText().trim());
                registerWordField.clear();
                refreshReports();
            } catch (SQLException exception) {
                log("Word registration failed: " + exception.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.add(new Label("Committed word"), 0, 0);
        form.add(autocompleteCommittedWordField, 1, 0);
        form.add(new Label("Suggestion limit"), 0, 1);
        form.add(suggestionLimitSpinner, 1, 1);

        VBox content = new VBox(18,
            titledLabel("Autocomplete"),
            new Label("Single-click a suggestion to append it to the draft sentence and immediately load the next suggestions."),
            form,
            requestSuggestionsButton,
            suggestionsView,
            new HBox(10, registerWordField, registerButton)
        );
        content.setPadding(new Insets(22));
        content.setStyle(cardStyle("#fbf1e8"));

        Tab tab = new Tab("Autocomplete", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createReportsTab() {
        reportSortBox = new ComboBox<>();
        reportSortBox.getItems().addAll(WordReportSort.values());
        reportSortBox.setValue(WordReportSort.ALPHABETICAL);

        reportWordLimitSpinner = new Spinner<>();
        reportWordLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 50));
        reportWordLimitSpinner.setEditable(true);

        reportSentenceLimitSpinner = new Spinner<>();
        reportSentenceLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 25));
        reportSentenceLimitSpinner.setEditable(true);

        duplicatesOnlyCheckBox = new CheckBox("Duplicates only");

        Button refreshButton = new Button("Refresh Reports");
        refreshButton.setOnAction(event -> refreshReports());

        TableView<WordReportView> reportTable = buildWordTable();
        ListView<String> sentenceHistoryList = new ListView<>(sentenceRows);
        sentenceHistoryList.setPrefHeight(240);
        sentenceHistoryList.setPlaceholder(new Label("Generated sentences will appear here."));

        GridPane controls = new GridPane();
        controls.setHgap(12);
        controls.setVgap(12);
        controls.add(new Label("Word sort"), 0, 0);
        controls.add(reportSortBox, 1, 0);
        controls.add(new Label("Word limit"), 2, 0);
        controls.add(reportWordLimitSpinner, 3, 0);
        controls.add(new Label("Sentence limit"), 0, 1);
        controls.add(reportSentenceLimitSpinner, 1, 1);
        controls.add(duplicatesOnlyCheckBox, 2, 1);
        controls.add(refreshButton, 3, 1);

        VBox content = new VBox(18,
            titledLabel("Reports"),
            new Label("Review imported word stats and your generated sentence history."),
            controls,
            reportTable,
            sentenceHistoryList
        );
        content.setPadding(new Insets(22));
        content.setStyle(cardStyle("#fcf2e8"));
        VBox.setVgrow(reportTable, Priority.ALWAYS);
        VBox.setVgrow(sentenceHistoryList, Priority.ALWAYS);

        Tab tab = new Tab("Reports", content);
        tab.setClosable(false);
        return tab;
    }

    private TableView<WordReportView> buildWordTable() {
        TableView<WordReportView> table = new TableView<>(wordRows);

        TableColumn<WordReportView, String> wordColumn = new TableColumn<>("Word");
        wordColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().wordText()));
        wordColumn.setPrefWidth(300);

        TableColumn<WordReportView, Integer> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().totalCount()));
        totalColumn.setPrefWidth(100);

        TableColumn<WordReportView, Integer> startColumn = new TableColumn<>("Starts");
        startColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().startCount()));
        startColumn.setPrefWidth(100);

        TableColumn<WordReportView, Integer> endColumn = new TableColumn<>("Ends");
        endColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().endCount()));
        endColumn.setPrefWidth(100);

        table.getColumns().addAll(wordColumn, totalColumn, startColumn, endColumn);
        return table;
    }

    private VBox createActivityLogPane() {
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        activityLog.setPrefRowCount(6);
        activityLog.setStyle("-fx-control-inner-background: #fff8f1; -fx-highlight-fill: #8f2d3a; -fx-highlight-text-fill: white;");

        VBox box = new VBox(8, titledLabel("Activity Log"), activityLog);
        box.setPadding(new Insets(14, 22, 22, 22));
        return box;
    }

    private void handleImport(String rawPath) {
        ImportViewState state = importController.validatePath(rawPath);
        importMessageLabel.setText(state.message());

        if (!state.valid()) {
            log("Import validation failed: " + state.message());
            return;
        }

        try {
            Path path = Path.of(rawPath.trim());
            ParseResult result = previewParser.parse(path);
            demoState.load(result, path);
            importMessageLabel.setText("Loaded " + result.getFileName() + ". The generate, autocomplete, and reports tabs are now ready.");
            updateSummary(result);
            setWorkspaceEnabled(true);
            sentenceDraftArea.clear();
            generateOutputArea.clear();
            autocompleteCommittedWordField.clear();
            suggestionsView.getItems().clear();
            refreshReports();
            log("Imported " + result.getFileName() + " at " + IMPORT_TIME_FORMATTER.format(result.getImportedAt()) + ".");
        } catch (IOException exception) {
            importMessageLabel.setText("Import preview failed: " + exception.getMessage());
            log("Import preview failed: " + exception.getMessage());
        }
    }

    private void handleGenerate() {
        String requestedStartWord = generateStartWordField.getText();
        String lastDraftWord = getLastDraftWord();
        boolean usingDraftContinuation = requestedStartWord == null || requestedStartWord.isBlank();
        String effectiveStartWord = usingDraftContinuation ? lastDraftWord : requestedStartWord;

        GenerateViewState state = generateController.generate(
            algorithmBox.getValue(),
            effectiveStartWord,
            maxWordsSpinner.getValue()
        );

        if (!state.success()) {
            generateOutputArea.clear();
            log("Generation failed: " + state.errorMessage());
            return;
        }

        generateOutputArea.setText(state.sentence());
        if (sentenceDraftArea.getText().isBlank()) {
            sentenceDraftArea.setText(state.sentence());
            log("Generated a new draft with " + algorithmBox.getValue().name().toLowerCase(Locale.ROOT) + " mode.");
        } else if (usingDraftContinuation && !lastDraftWord.isBlank()) {
            String continuation = removeLeadingWord(state.sentence(), lastDraftWord);
            if (!continuation.isBlank()) {
                appendTextToDraft(continuation);
                log("Generated continuation appended to the draft.");
            } else {
                log("Generation matched the draft seed word but did not add additional words.");
            }
        } else {
            log("Generated output is ready. Use 'Replace Draft with Output' if you want to swap it into the draft.");
        }

        refreshReports();
    }

    private void requestSuggestions(String committedWord, char commitChar, int limit, boolean userInitiated) {
        try {
            AutocompleteViewState state = autocompleteController.onWordCommitted(committedWord, commitChar, limit);
            suggestionsView.getItems().setAll(state.suggestions());
            if (state.suggestionsRequested()) {
                if (state.suggestions().isEmpty()) {
                    log("No suggestions found after '" + committedWord + "'.");
                } else if (userInitiated) {
                    log("Loaded " + state.suggestions().size() + " suggestions after '" + committedWord + "'.");
                }
            } else if (userInitiated) {
                log("Autocomplete skipped because '" + commitChar + "' is not a trigger character.");
            }
        } catch (SQLException exception) {
            log("Autocomplete failed: " + exception.getMessage());
        }
    }

    private void appendWordToDraft(String word) {
        if (word == null || word.isBlank()) {
            return;
        }

        String currentDraft = sentenceDraftArea.getText().trim();
        if (currentDraft.isBlank()) {
            sentenceDraftArea.setText(word.trim());
        } else {
            sentenceDraftArea.setText(currentDraft + " " + word.trim());
        }
        log("Added suggestion to draft: " + word);
    }

    private void appendTextToDraft(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String currentDraft = sentenceDraftArea.getText().trim();
        if (currentDraft.isBlank()) {
            sentenceDraftArea.setText(text.trim());
        } else {
            sentenceDraftArea.setText(currentDraft + " " + text.trim());
        }
    }

    private void removeLastWordFromDraft() {
        List<String> tokens = tokenizeDraft();
        if (tokens.isEmpty()) {
            log("Draft is already empty.");
            return;
        }
        tokens.remove(tokens.size() - 1);
        sentenceDraftArea.setText(String.join(" ", tokens));
        log("Removed the last word from the draft.");
    }

    private List<String> tokenizeDraft() {
        String trimmedDraft = sentenceDraftArea.getText().trim();
        if (trimmedDraft.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(trimmedDraft.split("\\s+")));
    }

    private String getLastDraftWord() {
        List<String> tokens = tokenizeDraft();
        for (int i = tokens.size() - 1; i >= 0; i--) {
            String normalized = normalizer.normalize(tokens.get(i));
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String removeLeadingWord(String sentence, String expectedFirstWord) {
        if (sentence == null || sentence.isBlank()) {
            return "";
        }

        List<String> tokens = new ArrayList<>(List.of(sentence.trim().split("\\s+")));
        if (tokens.isEmpty()) {
            return "";
        }

        String firstWord = normalizer.normalize(tokens.get(0));
        if (firstWord.equals(normalizer.normalize(expectedFirstWord))) {
            tokens.remove(0);
        }
        return String.join(" ", tokens);
    }

    private void refreshReports() {
        try {
            wordRows.setAll(reportsController.listWords(
                reportSortBox == null ? WordReportSort.ALPHABETICAL : reportSortBox.getValue(),
                reportWordLimitSpinner == null ? 50 : reportWordLimitSpinner.getValue()
            ));
            sentenceRows.setAll(reportsController.listGeneratedSentences(
                duplicatesOnlyCheckBox != null && duplicatesOnlyCheckBox.isSelected(),
                reportSentenceLimitSpinner == null ? 25 : reportSentenceLimitSpinner.getValue()
            ));
        } catch (SQLException exception) {
            log("Refreshing reports failed: " + exception.getMessage());
        }
    }

    private void refreshDraftMetadata() {
        String lastWord = getLastDraftWord();
        if (lastWord.isBlank()) {
            draftStatusValue.setText("Draft is empty");
            draftHelpValue.setText("Import a file, then build your sentence by generating text or clicking suggestions.");
        } else {
            draftStatusValue.setText("Last word: " + lastWord);
            draftHelpValue.setText("Blank generate/autocomplete inputs will use the draft's last word: " + lastWord);
        }
    }

    private void setWorkspaceEnabled(boolean enabled) {
        generateTab.setDisable(!enabled);
        autocompleteTab.setDisable(!enabled);
        reportsTab.setDisable(!enabled);
    }

    private void updateSummary(ParseResult result) {
        activeFileValue.setText(result.getFileName());
        statsValue.setText(
            "Words " + result.getTotalWords()
                + " | Sentences " + result.getTotalSentences()
                + " | Paragraphs " + result.getTotalParagraphs()
        );
    }

    private void log(String message) {
        if (activityLog.getText().isEmpty()) {
            activityLog.setText(message);
        } else {
            activityLog.appendText(System.lineSeparator() + message);
        }
    }

    private static Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #5b1f2a;");
        label.setWrapText(true);
        return label;
    }

    private static Label titledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");
        return label;
    }

    private static String cardStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + ";" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(111,29,42,0.18);" +
            "-fx-border-radius: 18;";
    }

    private static final class InMemoryAutocompleteGateway implements AutocompleteGateway {
        private final DemoUiState state;

        private InMemoryAutocompleteGateway(DemoUiState state) {
            this.state = state;
        }

        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) {
            return state.findSuggestions(normalizedWord, limit);
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            state.registerWord(normalizedWord);
        }
    }

    private static final class InMemoryReportingService implements UiReportingService {
        private final DemoUiState state;

        private InMemoryReportingService(DemoUiState state) {
            this.state = state;
        }

        @Override
        public List<WordReportView> listWords(WordReportSort sort, int limit) {
            return state.listWords(sort, limit);
        }

        @Override
        public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) {
            return state.listGeneratedSentences(onlyDuplicates, limit);
        }
    }

    private static final class DemoUiState {
        private final Normalizer normalizer = new Normalizer();
        private final Random random = new Random();
        private ParseResult parseResult;
        private final List<String> generatedSentences = new ArrayList<>();
        private final Set<String> registeredWords = new LinkedHashSet<>();

        private void load(ParseResult result, Path sourcePath) {
            this.parseResult = result;
            this.generatedSentences.clear();
            this.registeredWords.clear();
        }

        private String generateSentence(GenerationAlgorithm algorithm, String startWord, int maxWords) throws SQLException {
            if (parseResult == null) {
                throw new SQLException("Import a file before generating sentences.");
            }

            String currentWord = resolveStartWord(algorithm, startWord);
            if (currentWord == null || currentWord.isBlank()) {
                throw new SQLException("No words are available for generation.");
            }

            List<String> generated = new ArrayList<>();
            generated.add(currentWord);

            while (generated.size() < maxWords) {
                Map<String, Integer> nextWords = parseResult.getNextWordCounts().getOrDefault(currentWord, Map.of());
                if (nextWords.isEmpty()) {
                    break;
                }

                currentWord = algorithm == GenerationAlgorithm.GREEDY
                    ? chooseGreedy(nextWords)
                    : chooseWeighted(nextWords);

                if (currentWord == null || currentWord.isBlank()) {
                    break;
                }
                generated.add(currentWord);
            }

            String sentence = String.join(" ", generated);
            generatedSentences.add(sentence);
            return sentence;
        }

        private String resolveStartWord(GenerationAlgorithm algorithm, String startWord) {
            String normalizedStart = normalizer.normalize(startWord);
            if (!normalizedStart.isBlank() && parseResult.getWordCounts().containsKey(normalizedStart)) {
                return normalizedStart;
            }

            Map<String, Integer> sentenceStarts = parseResult.getSentenceStartCounts();
            if (!sentenceStarts.isEmpty()) {
                return algorithm == GenerationAlgorithm.GREEDY
                    ? chooseGreedy(sentenceStarts)
                    : chooseWeighted(sentenceStarts);
            }

            return chooseGreedy(parseResult.getWordCounts());
        }

        private String chooseGreedy(Map<String, Integer> options) {
            return options.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        }

        private String chooseWeighted(Map<String, Integer> options) {
            int totalWeight = options.values().stream()
                .filter(weight -> weight != null && weight > 0)
                .mapToInt(Integer::intValue)
                .sum();

            if (totalWeight <= 0) {
                return chooseGreedy(options);
            }

            int roll = random.nextInt(totalWeight);
            int runningTotal = 0;
            for (Map.Entry<String, Integer> entry : options.entrySet()) {
                int weight = entry.getValue();
                if (weight <= 0) {
                    continue;
                }
                runningTotal += weight;
                if (roll < runningTotal) {
                    return entry.getKey();
                }
            }
            return chooseGreedy(options);
        }

        private List<WeightedWord> findSuggestions(String normalizedWord, int limit) {
            if (parseResult == null || normalizedWord.isBlank()) {
                return List.of();
            }

            Map<String, Integer> nextWords = parseResult.getNextWordCounts().getOrDefault(normalizedWord, Map.of());
            return nextWords.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> new WeightedWord(entry.getKey().hashCode(), entry.getKey(), entry.getValue()))
                .toList();
        }

        private void registerWord(String normalizedWord) {
            if (!normalizedWord.isBlank()) {
                registeredWords.add(normalizedWord);
            }
        }

        private List<WordReportView> listWords(WordReportSort sort, int limit) {
            if (parseResult == null) {
                return List.of();
            }

            Set<String> allWords = new LinkedHashSet<>(parseResult.getWordCounts().keySet());
            allWords.addAll(registeredWords);

            Comparator<WordReportView> comparator = switch (sort) {
                case TOTAL_COUNT_DESC -> Comparator.comparingInt(WordReportView::totalCount).reversed()
                    .thenComparing(WordReportView::wordText);
                case START_COUNT_DESC -> Comparator.comparingInt(WordReportView::startCount).reversed()
                    .thenComparing(WordReportView::wordText);
                case END_COUNT_DESC -> Comparator.comparingInt(WordReportView::endCount).reversed()
                    .thenComparing(WordReportView::wordText);
                case ALPHABETICAL -> Comparator.comparing(WordReportView::wordText);
            };

            return allWords.stream()
                .map(word -> new WordReportView(
                    word,
                    parseResult.getWordCounts().getOrDefault(word, 0),
                    parseResult.getSentenceStartCounts().getOrDefault(word, 0),
                    parseResult.getSentenceEndCounts().getOrDefault(word, 0)
                ))
                .sorted(comparator)
                .limit(limit)
                .toList();
        }

        private List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) {
            if (!onlyDuplicates) {
                return generatedSentences.stream().limit(limit).toList();
            }

            Map<String, Long> counts = generatedSentences.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            return generatedSentences.stream()
                .filter(sentence -> counts.getOrDefault(sentence, 0L) > 1)
                .distinct()
                .limit(limit)
                .toList();
        }
    }
}
