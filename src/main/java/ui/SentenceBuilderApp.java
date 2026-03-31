/*
 * Class: SentenceBuilderApp
 * Original author: Archisha Sasson
 * Modified by: Omesh, Sammy
 *
 * Contributions:
 * - Original JavaFX preview application and import/generate/autocomplete/reports workflow by Archisha Sasson.
 * - UI layout refinements, responsive resizing behavior, draft pane improvements,
 *   import help/instructions, and fullscreen spacing polish by Omesh.
 * - Ranked autocomplete feedback, richer autocomplete UI states, and Tab-to-accept draft suggestions by Sammy.
 *
 * Description:
 * JavaFX preview application that surfaces the current ui package
 * progress through an import-first, draft-driven workflow.
 *
 * Example:
 * mvn javafx:run
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
import javafx.application.Application;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import parser.Normalizer;
import parser.ParseResult;
import parser.TextParser;
import parser.Tokenizer;

public class SentenceBuilderApp extends Application {
    private static final DateTimeFormatter IMPORT_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    // This preview app does not talk directly to the production database-backed UI flow.
    // Instead, it keeps a lightweight in-memory model so the team can demonstrate the
    // screens and controller interactions without requiring a full import pipeline.
    private final DemoUiState demoState = new DemoUiState();
    private final Normalizer normalizer = new Normalizer();
    // The preview parser reads imported text files and produces the statistics that feed
    // the demo generation, autocomplete, and reports tabs.
    private final TextParser previewParser = new TextParser(new Tokenizer(), new Normalizer(), false);
    private final ImportController importController = new ImportController();
    // Autocomplete goes through the real controller/service layer, but the gateway is an
    // in-memory adapter backed by DemoUiState instead of the database DAO.
    private final AutocompleteController autocompleteController =
        new AutocompleteController(new AutocompleteService(new InMemoryAutocompleteGateway(demoState)));
    // Generation also goes through the real controller/service contract. The two executors
    // are supplied as lambdas so the preview app can simulate weighted and greedy output.
    private final GenerateController generateController =
        new GenerateController(new GenerationService(
            (startWord, maxWords) -> demoState.generateSentence(GenerationAlgorithm.WEIGHTED, startWord, maxWords),
            (startWord, maxWords) -> demoState.generateSentence(GenerationAlgorithm.GREEDY, startWord, maxWords)
        ));
    // Reports are rendered from demoState so the UI can display imported word counts and
    // generated sentence history without depending on persistence.
    private final ReportsController reportsController =
        new ReportsController(new InMemoryReportingService(demoState));

    private final ObservableList<WordReportView> wordRows = FXCollections.observableArrayList();
    private final ObservableList<String> sentenceRows = FXCollections.observableArrayList();

    private final Label activeFileValue = createValueLabel("No file imported");
    private final Label statsValue = createValueLabel("Words 0 | Sentences 0 | Paragraphs 0");
    private final Label importMessageLabel = createValueLabel("Step 1: import a text file to unlock the rest of the workspace.");
    private final Label draftStatusValue = createValueLabel("Draft is empty");
    private final Label draftHelpValue = createValueLabel("Click autocomplete suggestions to build a sentence here.");
    private final Label draftSuggestionValue = createValueLabel("Top suggestion will appear here while you type in the draft.");
    private final Label autocompleteStatusValue = createValueLabel("Suggestions will appear here after you request them.");
    private final Label autocompletePlaceholderLabel = createValueLabel("Suggestions show up here after you request them.");
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
    private String draftTopSuggestion = "";

    // sammy 3/30: reuses the same empty-state message whenever autocomplete is reset after startup or a new import.
    private static final String DEFAULT_AUTOCOMPLETE_MESSAGE = "Suggestions show up here after you request them.";
    // sammy 3/30: keeps the live draft hint clear when there is not a suggestion ready to accept with tab.
    private static final String DEFAULT_DRAFT_SUGGESTION_MESSAGE = "Top suggestion will appear here while you type in the draft.";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        TabPane workspaceTabs = createWorkspaceTabs(stage);

        workspaceTabs.setPrefWidth(930);
        workspaceTabs.setMinWidth(820);
        workspaceTabs.setMaxWidth(980);

        VBox draftPane = createDraftPane();
        draftPane.setPrefWidth(400);
        draftPane.setMinWidth(360);
        draftPane.setMaxWidth(430);

        Region middleGap = new Region();
        HBox.setHgrow(middleGap, Priority.ALWAYS);

        HBox mainRow = new HBox(18, workspaceTabs, middleGap, draftPane);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox centerColumn = new VBox(16, createHeader(), mainRow, createActivityLogPane());
        VBox.setVgrow(mainRow, Priority.ALWAYS);   
        centerColumn.setPadding(new Insets(22, 28, 22, 28));
        centerColumn.setFillWidth(true);
        centerColumn.setMaxWidth(Double.MAX_VALUE);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setCenter(centerColumn);
        root.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f7efe3, #efe2d1);" +
            "-fx-font-family: 'Georgia';"
        );

        Scene scene = new Scene(root, 1320, 860);
        stage.setTitle("Team 43: Sentence Builder");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.show();

        workspaceTabs.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(workspaceTabs, Priority.ALWAYS);
        setWorkspaceEnabled(false);
        refreshDraftMetadata();
        refreshReports();
        log("UI preview started. Import a text file first, then build a draft with generation and autocomplete.");
    }

    private TabPane createWorkspaceTabs(Stage stage) {
        // Keep references to the non-import tabs so they can be enabled/disabled later.
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
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        Label subtitle = new Label("Interactive workspace for import, generation, autocomplete, and reports.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #8a3a46;");

        Label importLabel = new Label("Active Import");
        importLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        VBox summaryCard = new VBox(5, importLabel, activeFileValue, statsValue);
        summaryCard.setPadding(new Insets(12, 16, 12, 16));
        summaryCard.setPrefWidth(220);
        summaryCard.setMaxWidth(260);
        summaryCard.setStyle(
            "-fx-background-color: rgba(255,248,240,0.90);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(111,29,42,0.18);" +
            "-fx-border-radius: 14;"
        );

        VBox heading = new VBox(4, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(18, heading, spacer, summaryCard);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 4, 0));

        return new VBox(topBar);
    }

    private VBox createDraftPane() {
        // The draft pane is the shared "workspace" that generation and autocomplete both feed.
        Label title = titledLabel("Sentence Draft");
        Label instructions = new Label("Type directly, single-click an autocomplete suggestion, or press Tab to accept the top draft suggestion.");
        instructions.setWrapText(true);

        sentenceDraftArea.setWrapText(true);
        sentenceDraftArea.setPromptText("Your working sentence lives here...");
        sentenceDraftArea.setPrefRowCount(10);
        sentenceDraftArea.setPrefHeight(260);
        sentenceDraftArea.setMinHeight(220);
        sentenceDraftArea.setMaxHeight(420);
        // sammy 3/30: refreshes the draft status and the live top suggestion every time the user edits the draft.
        sentenceDraftArea.textProperty().addListener((obs, oldValue, newValue) -> {
            refreshDraftMetadata();
            refreshDraftSuggestionPreview();
        });
        sentenceDraftArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB && !draftTopSuggestion.isBlank()) {
                // sammy 3/30: lets tab accept the current top suggestion instead of inserting tab characters into the draft.
                event.consume();
                applySuggestionSelection(draftTopSuggestion);
            }
        });

        Button useLastWordForSuggestionsButton = new Button("Suggest from Last");
        useLastWordForSuggestionsButton.setOnAction(event -> {
            // This lets the user continue autocomplete from the current sentence draft rather
            // than manually copying the final word into the autocomplete form.
            String lastWord = getLastDraftWord();
            if (lastWord.isBlank()) {
                log("Draft is empty, so there is no last word to suggest from.");
                return;
            }
                autocompleteCommittedWordField.setText(lastWord);
                requestSuggestions(lastWord, ' ', suggestionLimitSpinner.getValue(), true);
        });

        Button useLastWordForGenerationButton = new Button("Generate from Last");
        useLastWordForGenerationButton.setOnAction(event -> {
            // Same idea as the suggestions button above, but for sentence generation.
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

        useLastWordForSuggestionsButton.setMaxWidth(Double.MAX_VALUE);
        useLastWordForGenerationButton.setMaxWidth(Double.MAX_VALUE);
        removeLastWordButton.setMaxWidth(Double.MAX_VALUE);
        clearDraftButton.setMaxWidth(Double.MAX_VALUE);

        HBox row1 = new HBox(8, useLastWordForSuggestionsButton, useLastWordForGenerationButton);
        HBox row2 = new HBox(8, removeLastWordButton, clearDraftButton);

        HBox.setHgrow(useLastWordForSuggestionsButton, Priority.ALWAYS);
        HBox.setHgrow(useLastWordForGenerationButton, Priority.ALWAYS);
        HBox.setHgrow(removeLastWordButton, Priority.ALWAYS);
        HBox.setHgrow(clearDraftButton, Priority.ALWAYS);

        VBox actions = new VBox(8, row1, row2);

        VBox box = new VBox(12,
            title,
            instructions,
            sentenceDraftArea,
            draftStatusValue,
            draftHelpValue,
            draftSuggestionValue,
            actions
        );
        box.setPadding(new Insets(18));
        box.setPrefWidth(380);
        box.setMinWidth(320);
        box.setMaxWidth(430);
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
            // Standard JavaFX file chooser used only to select a local text file.
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

        HBox fileRow = new HBox(10, pathField, browseButton, loadButton);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        Label requirementsTitle = new Label("File requirements");
        requirementsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        Label requirementsBody = new Label(
            "Use a plain .txt file containing normal readable text.\n" +
            "Good examples: copied article text, notes, paragraphs, short stories, or sample writing.\n" +
            "Do not use: PDF files, Word documents, images, or empty files.\n" +
            "After import, the app will count words/sentences and use that text for generation and autocomplete."
        );
        requirementsBody.setWrapText(true);
        requirementsBody.setStyle("-fx-text-fill: #6b3a42;");

        VBox helpBox = new VBox(8, requirementsTitle, requirementsBody);
        helpBox.setPadding(new Insets(14));
        helpBox.setStyle(
            "-fx-background-color: rgba(255,250,244,0.85);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(111,29,42,0.12);" +
            "-fx-border-radius: 12;"
        );

        VBox steps = new VBox(12,
            titledLabel("Import Preview"),
            new Label("1. Choose a text file."),
            new Label("2. Load it into the in-memory preview."),
            new Label("3. Use Generate and Autocomplete to build sentences from that imported data."),
            fileRow,
            importMessageLabel,
            helpBox
        );
        steps.setPadding(new Insets(18));
        steps.setStyle(cardStyle("#fdf4ea"));
        steps.setMaxWidth(920);
        steps.setFillWidth(true);

        VBox wrapper = new VBox(steps);
        wrapper.setFillWidth(true);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(steps, Priority.ALWAYS);

        steps.setMaxWidth(Double.MAX_VALUE);
        steps.setPrefWidth(Region.USE_COMPUTED_SIZE);

        Tab tab = new Tab("Import", wrapper);
        tab.setClosable(false);
        return tab;
    }

    private Tab createGenerateTab() {
        // The generate tab controls the algorithm and generation limits. Output is shown in
        // a read-only area first so the user can decide whether to copy it into the draft.
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
            // This explicitly replaces the draft. Generation itself may also append content,
            // but replacement is kept separate to make the action obvious in the demo.
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

        VBox content = new VBox(14,
            titledLabel("Sentence Generation"),
            new Label("Leave the start word blank and the app will continue from the last word in your draft when possible."),
            form,
            buttonRow,
            generateOutputArea
        );
        content.setPadding(new Insets(18));
        content.setStyle(cardStyle("#f9eee6"));

        Tab tab = new Tab("Generate", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createAutocompleteTab() {
        // The autocomplete tab asks for a committed word and returns candidate next words.
        // The user can click a suggestion to immediately append it to the draft.
        autocompleteCommittedWordField = new TextField();
        autocompleteCommittedWordField.setPromptText("Committed word. Leave blank to use the draft's last word.");

        suggestionLimitSpinner = new Spinner<>();
        suggestionLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        suggestionLimitSpinner.setEditable(true);

        suggestionsView = new ListView<>();
        suggestionsView.setPrefHeight(260);
        suggestionsView.setPlaceholder(autocompletePlaceholderLabel);
        suggestionsView.setCellFactory(listView -> createSuggestionCell());

        Button requestSuggestionsButton = new Button("Get Suggestions");
        requestSuggestionsButton.setOnAction(event -> {
            // If the field is blank, autocomplete uses the last normalized word from the draft.
            String committedWord = autocompleteCommittedWordField.getText().isBlank()
                ? getLastDraftWord()
                : autocompleteCommittedWordField.getText();
            if (committedWord.isBlank()) {
                // sammy 3/30: shows the same blank-input feedback in the ui that the controller uses for other autocomplete states.
                AutocompleteViewState blankState = AutocompleteViewState.blankInput();
                renderAutocompleteState(blankState);
                log(blankState.feedbackMessage());
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
            // This demonstrates the "ensure word exists" behavior in the autocomplete flow.
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

        VBox content = new VBox(14,
            titledLabel("Autocomplete"),
            new Label("Single-click a ranked suggestion to append it to the draft sentence and immediately load the next suggestions."),
            form,
            requestSuggestionsButton,
            autocompleteStatusValue,
            suggestionsView,
            new HBox(10, registerWordField, registerButton)
        );
        content.setPadding(new Insets(18));
        content.setStyle(cardStyle("#fbf1e8"));

        Tab tab = new Tab("Autocomplete", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createReportsTab() {
        // Reports tab presents the demo's imported word statistics and generated sentence
        // history so the user can inspect what the preview currently "knows."
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
        sentenceHistoryList.setPrefHeight(180);
        sentenceHistoryList.setMaxHeight(220);
        reportTable.setPrefHeight(260);
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

        VBox content = new VBox(14,
            titledLabel("Reports"),
            new Label("Review imported word stats and your generated sentence history."),
            controls,
            reportTable,
            sentenceHistoryList
        );
        content.setPadding(new Insets(18));
        content.setStyle(cardStyle("#fcf2e8"));
        VBox.setVgrow(reportTable, Priority.ALWAYS);
        VBox.setVgrow(sentenceHistoryList, Priority.ALWAYS);

        Tab tab = new Tab("Reports", content);
        tab.setClosable(false);
        return tab;
    }

    private TableView<WordReportView> buildWordTable() {
        // The table is intentionally read-only. It is a reporting surface, not an editor.
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
        activityLog.setPrefRowCount(4);
        activityLog.setPrefHeight(120);
        activityLog.setMinHeight(100);
        activityLog.setMaxHeight(150);
        activityLog.setStyle("-fx-control-inner-background: #fff8f1; -fx-highlight-fill: #8f2d3a; -fx-highlight-text-fill: white;");

        VBox box = new VBox(8, titledLabel("Activity Log"), activityLog);
        box.setPadding(new Insets(12, 16, 16, 16));
        box.setStyle(cardStyle("#fdf5ec"));
        return box;
    }

    private void handleImport(String rawPath) {
        // First validate using the real import controller so the UI preview follows the same
        // input rules as the rest of the application.
        ImportViewState state = importController.validatePath(rawPath);
        importMessageLabel.setText(state.message());

        String trimmedPath = rawPath == null ? "" : rawPath.trim();
        if (!trimmedPath.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            importMessageLabel.setText("Please choose a .txt file. PDF, DOCX, and other file types are not supported.");
            log("Import validation failed: unsupported file type.");
            return;
        }

        try {
            Path path = Path.of(rawPath.trim());
            // Parse the file and move its data into the in-memory preview model.
            ParseResult result = previewParser.parse(path);
            demoState.load(result, path);
            importMessageLabel.setText("Loaded " + result.getFileName() + ". The generate, autocomplete, and reports tabs are now ready.");
            updateSummary(result);
            setWorkspaceEnabled(true);
            sentenceDraftArea.clear();
            generateOutputArea.clear();
            autocompleteCommittedWordField.clear();
            resetAutocompleteSuggestions();
            clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            refreshReports();
            log("Imported " + result.getFileName() + " at " + IMPORT_TIME_FORMATTER.format(result.getImportedAt()) + ".");
        } catch (IOException exception) {
            importMessageLabel.setText(
                "Import failed. Please choose a plain .txt file with readable text content."
            );
            log("Import preview failed: " + exception.getMessage());
        }
    }

    private void handleGenerate() {
        // Blank start-word input means "continue from the current draft" when possible.
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

        // The generation tab always shows raw output first, even if the draft is also updated.
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
            // The controller decides whether suggestions should be requested for the given
            // committed character and returns UI-ready string suggestions.
            AutocompleteViewState state = autocompleteController.onWordCommitted(committedWord, commitChar, limit);
            renderAutocompleteState(state);

            // sammy 3/30: keeps automatic follow-up suggestion refreshes quiet unless they end with no suggestions.
            if (shouldLogAutocompleteState(state, userInitiated)) {
                log(state.feedbackMessage());
            }
        } catch (SQLException exception) {
            log("Autocomplete failed: " + exception.getMessage());
        }
    }

    private void appendWordToDraft(String word) {
        // Suggestions are appended with whitespace normalization so the draft stays readable.
        if (word == null || word.isBlank()) {
            return;
        }

        String currentDraft = sentenceDraftArea.getText().trim();
        if (currentDraft.isBlank()) {
            sentenceDraftArea.setText(word.trim());
        } else {
            sentenceDraftArea.setText(currentDraft + " " + word.trim());
        }
        // sammy 3/30: moves the caret to the end so the draft still feels correct if the user wants to keep typing.
        sentenceDraftArea.positionCaret(sentenceDraftArea.getText().length());
        log("Added suggestion to draft: " + word);
    }

    private void appendTextToDraft(String text) {
        // Shared helper used when generated text should extend the existing draft.
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
        // Simple editing affordance for demo use when the user wants to backtrack one token.
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
        // Draft tokenization is intentionally lightweight because this is just UI-side text
        // manipulation, not the canonical parser pipeline.
        String trimmedDraft = sentenceDraftArea.getText().trim();
        if (trimmedDraft.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(trimmedDraft.split("\\s+")));
    }

    private String getLastDraftWord() {
        // Walk backward so trailing punctuation or blank fragments do not confuse downstream
        // generation/autocomplete requests.
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
        // When generation continues from the draft's last word, we remove the duplicated seed
        // word before appending the continuation back into the draft.
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

    // sammy 3/30: keeps the draft pane showing the best next-word suggestion while the user types in the draft.
    private void refreshDraftSuggestionPreview() {
        if (autocompleteTab == null || autocompleteTab.isDisabled()) {
            clearDraftSuggestionPreview("Import a file to see live draft suggestions.");
            return;
        }

        String lastWord = getLastDraftWord();
        if (lastWord.isBlank()) {
            resetAutocompleteSuggestions();
            clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            return;
        }

        try {
            AutocompleteViewState state = autocompleteController.onWordCommitted(
                lastWord,
                ' ',
                suggestionLimitSpinner == null ? 5 : suggestionLimitSpinner.getValue()
            );
            renderAutocompleteState(state);

            // sammy 3/30: surfaces only the first suggestion as the quick tab-to-accept preview in the draft pane.
            if (state.hasSuggestions()) {
                draftTopSuggestion = state.suggestions().get(0);
                draftSuggestionValue.setText("Top suggestion: " + draftTopSuggestion + " (press Tab to accept)");
            } else if (state.outcome() == AutocompleteViewState.AutocompleteOutcome.NO_RESULTS) {
                clearDraftSuggestionPreview("No top suggestion found after '" + lastWord + "'.");
            } else {
                clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            }
        } catch (SQLException exception) {
            clearDraftSuggestionPreview("Live suggestions failed: " + exception.getMessage());
        }
    }

    // sammy 3/30: renders each suggestion as a ranked row and ignores clicks on empty space inside the list view.
    private ListCell<String> createSuggestionCell() {
        ListCell<String> cell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // sammy 3/30: shows the service-provided order directly in the ui so users can see the ranking clearly.
                setText((getIndex() + 1) + ". " + item);
            }
        };

        cell.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1 || cell.isEmpty()) {
                return;
            }

            applySuggestionSelection(cell.getItem());
        });
        return cell;
    }

    // sammy 3/30: keeps the click flow in one place so adding a suggestion and loading the next list stay in sync.
    private void applySuggestionSelection(String selectedWord) {
        if (selectedWord == null || selectedWord.isBlank()) {
            return;
        }

        appendWordToDraft(selectedWord);
        autocompleteCommittedWordField.setText(selectedWord);
        suggestionsView.getSelectionModel().clearSelection();
        requestSuggestions(selectedWord, ' ', suggestionLimitSpinner.getValue(), false);
    }

    // sammy 3/30: updates the list, empty-state text, and status label together so autocomplete feedback stays consistent.
    private void renderAutocompleteState(AutocompleteViewState state) {
        suggestionsView.getItems().setAll(state.suggestions());
        suggestionsView.getSelectionModel().clearSelection();
        autocompleteStatusValue.setText(state.feedbackMessage());

        if (state.hasSuggestions()) {
            autocompletePlaceholderLabel.setText("Suggestions are ready. Click one to keep building your draft.");
            suggestionsView.scrollTo(0);
            return;
        }

        autocompletePlaceholderLabel.setText(state.feedbackMessage().isBlank() ? DEFAULT_AUTOCOMPLETE_MESSAGE : state.feedbackMessage());
    }

    // sammy 3/30: avoids noisy log spam while still surfacing important autocomplete outcomes to the activity log.
    private boolean shouldLogAutocompleteState(AutocompleteViewState state, boolean userInitiated) {
        return switch (state.outcome()) {
            case SHOW_RESULTS, SKIPPED_TRIGGER, BLANK_INPUT -> userInitiated;
            case NO_RESULTS -> true;
        };
    }

    // sammy 3/30: restores the default autocomplete messaging when a new import clears the current suggestion session.
    private void resetAutocompleteSuggestions() {
        suggestionsView.getItems().clear();
        suggestionsView.getSelectionModel().clearSelection();
        autocompleteStatusValue.setText(DEFAULT_AUTOCOMPLETE_MESSAGE);
        autocompletePlaceholderLabel.setText(DEFAULT_AUTOCOMPLETE_MESSAGE);
    }

    // sammy 3/30: clears the saved tab suggestion and replaces it with a plain-language hint for the draft pane.
    private void clearDraftSuggestionPreview(String message) {
        draftTopSuggestion = "";
        draftSuggestionValue.setText(message);
    }

    private void refreshReports() {
        // Refresh both reporting surfaces together so they always describe the same preview state.
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
        // The metadata labels coach the user on what the draft currently contains and how
        // blank generate/autocomplete inputs will be interpreted.
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
        // Import tab remains available at all times; the others are gated by import success.
        generateTab.setDisable(!enabled);
        autocompleteTab.setDisable(!enabled);
        reportsTab.setDisable(!enabled);
    }

    private void updateSummary(ParseResult result) {
        // Summary card mirrors the most recently imported parse statistics.
        activeFileValue.setText(result.getFileName());
        statsValue.setText(
            "Words " + result.getTotalWords()
                + " | Sentences " + result.getTotalSentences()
                + " | Paragraphs " + result.getTotalParagraphs()
        );
    }

    private void log(String message) {
        // Append-only log so observers can follow the demo session chronologically.
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
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(111,29,42,0.14);" +
            "-fx-border-radius: 16;";
    }

    private static final class InMemoryAutocompleteGateway implements AutocompleteGateway {
        private final DemoUiState state;

        private InMemoryAutocompleteGateway(DemoUiState state) {
            this.state = state;
        }

        @Override
        public List<WeightedWord> findNextWordSuggestions(String normalizedWord, int limit) {
            // Delegates directly into the preview state's in-memory next-word model.
            return state.findSuggestions(normalizedWord, limit);
        }

        @Override
        public void ensureWordExists(String normalizedWord) {
            // Registration in the preview just adds the word to a local set so it can show up
            // in reports without touching the database.
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
            // Reports are computed from the imported parse result plus any user-registered words.
            return state.listWords(sort, limit);
        }

        @Override
        public List<String> listGeneratedSentences(boolean onlyDuplicates, int limit) {
            return state.listGeneratedSentences(onlyDuplicates, limit);
        }
    }

    private static final class DemoUiState {
        // DemoUiState is the preview application's in-memory "model layer."
        // It stores the imported parse result, generated sentence history, and user-registered
        // autocomplete words so the UI can behave like a real app during demonstrations.
        private final Normalizer normalizer = new Normalizer();
        private final Random random = new Random();
        private ParseResult parseResult;
        private final List<String> generatedSentences = new ArrayList<>();
        private final Set<String> registeredWords = new LinkedHashSet<>();

        private void load(ParseResult result, Path sourcePath) {
            // Loading a new file replaces the current preview state instead of merging it.
            this.parseResult = result;
            this.generatedSentences.clear();
            this.registeredWords.clear();
        }

        private String generateSentence(GenerationAlgorithm algorithm, String startWord, int maxWords) throws SQLException {
            // Generation runs entirely against the imported parse result maps, not the database.
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
                // Each step looks up the current word's possible transitions from ParseResult.
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
            // Generated sentences are saved in-memory so the reports tab can display history.
            generatedSentences.add(sentence);
            return sentence;
        }

        private String resolveStartWord(GenerationAlgorithm algorithm, String startWord) {
            // Prefer an explicit valid start word; otherwise fall back to sentence starts or,
            // failing that, the most common word in the imported result.
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
            // Greedy mode is deterministic: highest count wins, then alphabetical tie-break.
            return options.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        }

        private String chooseWeighted(Map<String, Integer> options) {
            // Weighted mode simulates probabilistic next-word selection using frequencies.
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
            // Suggestions come from the same next-word counts used for generation, but they are
            // converted into WeightedWord records so the controller/service contract stays intact.
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
            // Registered words are tracked separately so they can appear in reports even if the
            // imported file did not contain them.
            if (!normalizedWord.isBlank()) {
                registeredWords.add(normalizedWord);
            }
        }

        private List<WordReportView> listWords(WordReportSort sort, int limit) {
            // Reports blend imported word counts with registered words so the UI reflects both
            // passive data (from the file) and active user interaction.
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
            // Duplicate filtering helps demonstrate whether repeated generation calls are
            // converging on the same sentence.
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
