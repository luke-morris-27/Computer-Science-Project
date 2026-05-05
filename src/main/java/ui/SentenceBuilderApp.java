/*
 * Class: SentenceBuilderApp
 * Original author: Archisha Sasson
 * Modified by: Archisha Sasson, Omesh, Sammy, Luke
 *
 * Contributions:
 * - Original JavaFX preview application and import/generate/autocomplete/reports workflow by Archisha Sasson.
 * - Layout sizing adjustments to draft/generation balance, autocomplete suggestion display,
 *   header visibility, fit-to-window scaling, and typography tuning by Archisha Sasson.
 * - UI layout refinements, responsive resizing behavior, draft pane improvements,
 *   import help/instructions, and fullscreen spacing polish by Omesh.
 * - Ranked autocomplete feedback, richer autocomplete UI states, and Tab-to-accept draft suggestions by Sammy.
 * - Random generation mode and refactored generation service to support multiple algorithms by Omesh.
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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import db.AutocompleteDao;
import db.DbGeneratorRepository;
import db.DbReportingService;
import generator.AutocompleteService;
import generator.GenerationAlgorithm;
import generator.GenerationService;
import generator.GeneratorRepository;
import generator.GreedyGenerator;
import generator.RandomGenerator;
import generator.WeightedGenerator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
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
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import parser.DatabaseConfig;
import parser.ImportService;
import parser.ImportPreparationResult;
import parser.ImportPreparationStatus;
import parser.Normalizer;
import parser.ParseResult;
import parser.TextParser;
import parser.Tokenizer;
import parser.WordDb;

public class SentenceBuilderApp extends Application {
    private static final DateTimeFormatter IMPORT_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    // Archisha: use a fixed design canvas that scales to fit the stage so the full UI stays visible.
    private static final double BASE_WINDOW_WIDTH = 1660;
    private static final double BASE_WINDOW_HEIGHT = 920;
    //Code by Archisha Sasson
    private static final double DRAFT_GHOST_MARGIN = 10;
    private static final double DRAFT_GHOST_CARET_GAP = 2;
    // Code by Shriram
    // separates multiple file paths inside the import path field so the load handler can split them back apart
    private static final String MULTI_PATH_SEPARATOR = ";";
    // End of Code by Shriram

    private static final String DRAFT_TEXT_STYLE = "-fx-font-family: 'Georgia'; -fx-font-size: 16px;";
    private static final String DRAFT_GHOST_TEXT_STYLE = DRAFT_TEXT_STYLE + "-fx-text-fill: #7a7a7a;";
    //End of Code by Archisha Sasson

    private final Normalizer normalizer = new Normalizer();
    // Parser is pure; ImportService owns the transaction so each import is all-or-nothing.
    private final TextParser importParser = new TextParser(new Tokenizer(), new Normalizer());
    private final ImportService importService = new ImportService(importParser);
    private final ImportController importController = new ImportController();
    private final GeneratorRepository uiGeneratorRepository = new DbGeneratorRepository();
    private final AutocompleteController autocompleteController =
        new AutocompleteController(new AutocompleteService(new AutocompleteDao()));
    private final WeightedGenerator weightedGenerator =
        new WeightedGenerator(uiGeneratorRepository, new Random(), normalizer);
    private final GreedyGenerator greedyGenerator =
        new GreedyGenerator(uiGeneratorRepository, normalizer);
    private final RandomGenerator randomGenerator =
        new RandomGenerator(uiGeneratorRepository, new Random(), normalizer);

    private final GenerateController generateController =
        new GenerateController(new GenerationService(
            weightedGenerator::generateWeighted,
            greedyGenerator::generateGreedy,
            randomGenerator::generateRandom
        ));
    private final ReportsController reportsController =
        new ReportsController(new DbReportingService());

    private final ObservableList<WordReportView> wordRows = FXCollections.observableArrayList();
    private final ObservableList<String> sentenceRows = FXCollections.observableArrayList();

    private final Label activeFileValue = createValueLabel("No file imported");
    private final Label statsValue = createValueLabel("Words 0 | Sentences 0 | Paragraphs 0");
    private final Label importMessageLabel = createValueLabel("Step 1: import a text file to unlock the rest of the workspace.");
    private final Label draftStatusValue = createValueLabel("Draft is empty");
    private final Label draftHelpValue = createValueLabel("Click autocomplete suggestions to build a sentence here.");
    // sammy 4/7: making the draft suggeston label an overlay inside the draft editor
    private final Label draftSuggestionValue = new Label();
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
    //Code by Archisha Sasson
    private Pane draftSuggestionOverlay;
    //End of Code by Archisha Sasson

    private TextField reportSearchField;
    private TextField reportSecondWordField;
    private ComboBox<WordReportSort> reportSortBox;
    private Spinner<Integer> reportWordLimitSpinner;
    private Spinner<Integer> reportSentenceLimitSpinner;
    private CheckBox duplicatesOnlyCheckBox;
    private String draftTopSuggestion = "";
    //Code by Archisha Sasson
    private String displayedDraftSuggestion = "";
    //End of Code by Archisha Sasson
    // sammy 4/7: pauses the draft preview refresh for a moment when tab accepts a suggestion so the old hint does not flash back.
    private boolean suppressDraftSuggestionPreview;

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

        workspaceTabs.setPrefWidth(660);
        workspaceTabs.setMinWidth(500);
        workspaceTabs.setMaxWidth(Double.MAX_VALUE);

        VBox draftPane = createDraftPane();
        draftPane.setPrefWidth(760);
        draftPane.setMinWidth(460);
        draftPane.setMaxWidth(Double.MAX_VALUE);

        HBox mainRow = new HBox(22, workspaceTabs, draftPane);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox centerColumn = new VBox(16, createHeader(), mainRow, createActivityLogPane());
        VBox.setVgrow(mainRow, Priority.ALWAYS);   
        centerColumn.setPadding(new Insets(22, 28, 22, 28));
        centerColumn.setFillWidth(true);
        centerColumn.setMaxWidth(Double.MAX_VALUE);

        // Archisha: keep the designed workspace at a stable base size, then scale it responsively with the window.
        BorderPane contentRoot = new BorderPane();
        contentRoot.setPadding(new Insets(18, 20, 18, 20));
        contentRoot.setCenter(centerColumn);
        contentRoot.setPrefSize(BASE_WINDOW_WIDTH, BASE_WINDOW_HEIGHT);
        contentRoot.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        contentRoot.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        contentRoot.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 16px;");

        StackPane root = new StackPane(contentRoot);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f7efe3, #efe2d1);");

        // Archisha: fit the whole workspace into the available stage instead of clipping individual panels.
        DoubleBinding scaleBinding = Bindings.createDoubleBinding(
            () -> Math.min(
                1.0,
                Math.min(
                    root.getWidth() / BASE_WINDOW_WIDTH,
                    root.getHeight() / BASE_WINDOW_HEIGHT
                )
            ),
            root.widthProperty(),
            root.heightProperty()
        );
        contentRoot.scaleXProperty().bind(scaleBinding);
        contentRoot.scaleYProperty().bind(scaleBinding);

        Scene scene = new Scene(root, BASE_WINDOW_WIDTH, BASE_WINDOW_HEIGHT);
        stage.setTitle("Team 43: Sentence Builder");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(720);
        stage.setMinHeight(520);
        stage.show();

        workspaceTabs.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(workspaceTabs, Priority.ALWAYS);
        HBox.setHgrow(draftPane, Priority.ALWAYS);
        boolean hasExistingData = databaseHasWorkspaceData();
        setWorkspaceEnabled(hasExistingData);
        refreshDraftMetadata();
        logStartupDatabaseStatus();
        if (hasExistingData) {
            loadStartupSummaryFromDatabase();
            importMessageLabel.setText("Database already contains imported data. You can generate, autocomplete, and view reports immediately.");
        }
        refreshReports();
    }

    /**
     * Opens one JDBC connection so the activity log shows a real success/failure (not just a static string).
     */
    private void logStartupDatabaseStatus() {
        try (Connection conn = WordDb.openConnection()) {
            if (!conn.isValid(3)) {
                log("Database: connection opened but validation failed. Check MySQL is running and credentials in .env.");
                return;
            }
            log(
                "Database: connected to MySQL as "
                    + DatabaseConfig.resolveUsername()
                    + " ("
                    + DatabaseConfig.resolveJdbcUrl()
                    + "). Import a .txt file first, then build a draft with generation and autocomplete."
            );
        } catch (SQLException e) {
            log(
                "Database: could not connect — "
                    + e.getMessage()
                    + ". Check .env (or env vars), that MySQL is running, and that you ran database/SentenceBuilderDatabase.sql."
            );
        }
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
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        Label subtitle = new Label("Interactive workspace for import, generation, autocomplete, and reports.");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #8a3a46;");

        Label importLabel = new Label("Active Import");
        importLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        // Archisha: widen the import summary card so the active file details have enough room on the top right.
        VBox summaryCard = new VBox(6, importLabel, activeFileValue, statsValue);
        summaryCard.setPadding(new Insets(14, 18, 14, 18));
        summaryCard.setMinWidth(280);
        summaryCard.setPrefWidth(380);
        summaryCard.setMaxWidth(440);
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
        Label title = titledLabel("Sentence Draft");
        Label instructions = new Label("Type directly, single-click an autocomplete suggestion, or press Tab to accept the top draft suggestion.");
        instructions.setWrapText(true);

        sentenceDraftArea.setWrapText(true);
        sentenceDraftArea.setPromptText("Your working sentence lives here...");
        sentenceDraftArea.setPrefRowCount(14);
        sentenceDraftArea.setPrefColumnCount(38);
        sentenceDraftArea.setPrefHeight(380);
        sentenceDraftArea.setMinHeight(340);
        sentenceDraftArea.setMaxHeight(560);
        sentenceDraftArea.setMaxWidth(Double.MAX_VALUE);
        //Code by Archisha Sasson
        sentenceDraftArea.setStyle(DRAFT_TEXT_STYLE);
        //End of Code by Archisha Sasson
        // sammy 4/7: turns the draft suggestion into gray ghost text that can sit inside the draft box near the caret.
        //Code by Archisha Sasson
        draftSuggestionValue.setManaged(false);
        //End of Code by Archisha Sasson
        draftSuggestionValue.setMouseTransparent(true);
        draftSuggestionValue.setWrapText(false);
        draftSuggestionValue.setVisible(false);
        //Code by Archisha Sasson
        draftSuggestionValue.setStyle(DRAFT_GHOST_TEXT_STYLE);
        //End of Code by Archisha Sasson
        // sammy 3/30: refreshes the draft status and the live top suggestion every time the user edits the draft.
        sentenceDraftArea.textProperty().addListener((obs, oldValue, newValue) -> {
            refreshDraftMetadata();
            // sammy 4/7: skips the automatic preview refresh while tab is accepting a suggestion so the old hint does not flash back.
            if (suppressDraftSuggestionPreview) {
                return;
            }
            refreshDraftSuggestionPreview();
        });
        //Code by Archisha Sasson
        // The ghost suggestion follows the real caret inside the draft box.
        sentenceDraftArea.caretPositionProperty().addListener((obs, oldValue, newValue) -> updateDraftSuggestionVisibility());
        sentenceDraftArea.scrollTopProperty().addListener((obs, oldValue, newValue) -> queueDraftSuggestionPosition());
        sentenceDraftArea.scrollLeftProperty().addListener((obs, oldValue, newValue) -> queueDraftSuggestionPosition());
        sentenceDraftArea.widthProperty().addListener((obs, oldValue, newValue) -> queueDraftSuggestionPosition());
        sentenceDraftArea.heightProperty().addListener((obs, oldValue, newValue) -> queueDraftSuggestionPosition());
        //End of Code by Archisha Sasson
        sentenceDraftArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            //Code by Archisha Sasson
            if (event.getCode() != KeyCode.TAB) {
                return;
            }

            event.consume();

            if (displayedDraftSuggestion == null || displayedDraftSuggestion.isBlank()) {
                return;
            }

            if (!isCaretAtDraftEnd()) {
                return;
            }

            String suggestionToAccept = displayedDraftSuggestion;
            applySuggestionSelection(suggestionToAccept, true);
            //End of Code by Archisha Sasson
        });

        //Code by Archisha Sasson
        // The overlay is inside the same draft box, so the ghost word appears after the typed text.
        draftSuggestionOverlay = new Pane(draftSuggestionValue);
        draftSuggestionOverlay.setMouseTransparent(true);
        draftSuggestionOverlay.setPickOnBounds(false);
        draftSuggestionOverlay.prefWidthProperty().bind(sentenceDraftArea.widthProperty());
        draftSuggestionOverlay.prefHeightProperty().bind(sentenceDraftArea.heightProperty());
        StackPane draftEditor = new StackPane(sentenceDraftArea, draftSuggestionOverlay);
        StackPane.setAlignment(draftSuggestionOverlay, Pos.TOP_LEFT);
        //End of Code by Archisha Sasson

        Button useLastWordForSuggestionsButton = new Button("Suggest from Last");
        useLastWordForSuggestionsButton.setOnAction(event -> {
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
            draftEditor,
            draftStatusValue,
            draftHelpValue,
            actions
        );
        box.setPadding(new Insets(18));
        box.setPrefWidth(760);
        box.setMinWidth(460);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setFillWidth(true);
        box.setStyle(cardStyle("#fff7ee"));
        VBox.setVgrow(draftEditor, Priority.ALWAYS);
        return box;
    }

    private Tab createImportTab(Stage stage) {
        TextField pathField = new TextField();
        // Code by Shriram
        // updates the placeholder so users know they can pick more than one file at once
        pathField.setPromptText("Select one or more .txt files to validate and preview");
        // End of Code by Shriram
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(event -> {
            // Code by Shriram
            // standard JavaFX multi-file chooser so the user can import several text files in one go
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Text Files");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.*"));
            java.util.List<java.io.File> selectedFiles = chooser.showOpenMultipleDialog(stage);
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                // joins the selected paths with a separator the load handler can split back apart
                String joined = selectedFiles.stream()
                    .map(java.io.File::getAbsolutePath)
                    .collect(java.util.stream.Collectors.joining(MULTI_PATH_SEPARATOR));
                pathField.setText(joined);
            }
            // End of Code by Shriram
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
        algorithmBox.getItems().addAll(
            GenerationAlgorithm.WEIGHTED,
            GenerationAlgorithm.GREEDY,
            GenerationAlgorithm.RANDOM
        );
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
        generateOutputArea.setPrefRowCount(5);
        generateOutputArea.setPrefHeight(170);
        generateOutputArea.setMinHeight(150);
        generateOutputArea.setMaxHeight(190);

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
        form.add(new Label("Generation Mode"), 0, 0);
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
            new Label("Single-click a suggestion to append it to the draft sentence and immediately load the next suggestions."),
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

        reportSearchField = new TextField();
        reportSearchField.setPromptText("Search words...");

        reportSecondWordField = new TextField();
        reportSecondWordField.setPromptText("Word relations to search word");

        reportWordLimitSpinner = new Spinner<>();
        reportWordLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 50));
        reportWordLimitSpinner.setEditable(true);
        reportWordLimitSpinner.setPrefWidth(220);

        reportSentenceLimitSpinner = new Spinner<>();
        reportSentenceLimitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 25));
        reportSentenceLimitSpinner.setEditable(true);
        reportSentenceLimitSpinner.setPrefWidth(220);

        duplicatesOnlyCheckBox = new CheckBox("Duplicates only");
        duplicatesOnlyCheckBox.setOnAction(event -> refreshReports());

        Button refreshButton = new Button("Refresh Reports");
        refreshButton.setOnAction(event -> refreshReports());
        refreshButton.setPrefWidth(180);


        TableView<WordReportView> reportTable = buildWordTable();
        ListView<String> sentenceHistoryList = new ListView<>(sentenceRows);
        sentenceHistoryList.setPrefHeight(180);
        sentenceHistoryList.setMaxHeight(220);
        reportTable.setPrefHeight(260);
        sentenceHistoryList.setPlaceholder(new Label("Generated sentences will appear here."));
        Label sentenceHistoryTitle = new Label("Generated Sentence History");
        sentenceHistoryTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");

        GridPane controls = new GridPane();
        controls.setHgap(12);
        controls.setVgap(12);
        Label sentenceLimitLabel = new Label("Sentence limit");
        sentenceLimitLabel.setPrefWidth(140);
        controls.add(new Label("Word sort"), 0, 0);
        controls.add(reportSortBox, 1, 0);
        controls.add(refreshButton, 2, 0);
        controls.add(new Label("Search"), 0, 1);
        controls.add(reportSearchField, 1, 1);
        controls.add(reportSecondWordField, 2, 1, 2, 1);
        controls.add(sentenceLimitLabel, 0, 2);
        controls.add(reportSentenceLimitSpinner, 1, 2);
        GridPane.setHalignment(reportSentenceLimitSpinner, HPos.RIGHT);
        controls.add(new Label("Word limit"), 2, 2);
        controls.add(reportWordLimitSpinner, 3, 2);

        VBox content = new VBox(14,
            titledLabel("Reports"),
            new Label("Review imported word stats and your generated sentence history."),
            controls,
            reportTable,
            sentenceHistoryTitle,
            duplicatesOnlyCheckBox,
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

        TableColumn<WordReportView, Integer> followsColumn = new TableColumn<>("Follows");
        followsColumn.setCellValueFactory(cell -> 
        new ReadOnlyObjectWrapper<>(cell.getValue().followsCount()));
        followsColumn.setPrefWidth(100);

        TableColumn<WordReportView, Integer> precedesColumn = new TableColumn<>("Precedes");
        precedesColumn.setCellValueFactory(cell -> 
        new ReadOnlyObjectWrapper<>(cell.getValue().precedesCount()));
        precedesColumn.setPrefWidth(100);

        table.getColumns().addAll(wordColumn, totalColumn, startColumn, endColumn, followsColumn, precedesColumn);
        

        return table;
    }

    private VBox createActivityLogPane() {
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        // Archisha: keep the activity log compact so more vertical space stays available for the main workspace.
        activityLog.setPrefRowCount(5);
        activityLog.setPrefHeight(145);
        activityLog.setMinHeight(130);
        activityLog.setMaxHeight(190);
        activityLog.setStyle("-fx-control-inner-background: #fff8f1; -fx-highlight-fill: #8f2d3a; -fx-highlight-text-fill: white;");

        VBox box = new VBox(8, titledLabel("Activity Log"), activityLog);
        box.setPadding(new Insets(12, 16, 20, 16));
        box.setStyle(cardStyle("#fdf5ec"));
        return box;
    }

    // Code by Shriram
    // accepts a single raw path or a separator-joined list of paths so a single browse can import multiple files in one click
    private void handleImport(String rawPathOrPaths) {
        if (rawPathOrPaths == null || rawPathOrPaths.isBlank()) {
            ImportViewState state = importController.validatePath(rawPathOrPaths);
            importMessageLabel.setText(state.message());
            return;
        }

        // splits the joined path string back into individual paths so each file can be processed independently
        java.util.List<String> rawPaths = java.util.Arrays.stream(rawPathOrPaths.split(MULTI_PATH_SEPARATOR))
            .map(String::trim)
            .filter(text -> !text.isEmpty())
            .toList();

        int successCount = 0;
        int duplicateCount = 0;
        int failureCount = 0;
        ParseResult lastImported = null;

        for (String rawPath : rawPaths) {
            // basic path validation reuses the original controller logic so single-file behavior is unchanged
            ImportViewState pathState = importController.validatePath(rawPath);
            if (!pathState.valid()) {
                failureCount++;
                log("Skipped " + rawPath + ": " + pathState.message());
                continue;
            }

            // file extension check happens once per file so a bad mime in the middle of a batch does not abort the rest
            if (!rawPath.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                failureCount++;
                log("Skipped " + rawPath + ": only .txt files are supported.");
                continue;
            }

            Path path = Path.of(rawPath);

            final ImportPreparationResult prep;
            try {
                prep = importController.prepareImport(path);
            } catch (IOException | SQLException prepException) {
                failureCount++;
                log("Skipped " + path.getFileName() + ": " + prepException.getMessage());
                continue;
            }

            if (prep.status() == ImportPreparationStatus.DUPLICATE) {
                duplicateCount++;
                log("Skipped " + path.getFileName() + ": this file has already been imported.");
                continue;
            }
            if (!prep.readyToImport()) {
                failureCount++;
                log("Skipped " + path.getFileName() + ": " + prep.message());
                continue;
            }

            try {
                ParseResult result = importService.importFile(path, prep.fileHash());
                lastImported = result;
                successCount++;
                log("Imported " + result.getFileName() + " at " + IMPORT_TIME_FORMATTER.format(result.getImportedAt()) + ".");
            } catch (IOException | SQLException exception) {
                failureCount++;
                log("Import failed for " + path.getFileName() + ": " + exception.getMessage());
            }
        }

        // builds a single summary message so the import label reflects the whole batch instead of the last file alone
        StringBuilder summary = new StringBuilder();
        summary.append("Imported ").append(successCount).append(successCount == 1 ? " file" : " files");
        if (duplicateCount > 0) {
            summary.append(", skipped ").append(duplicateCount).append(duplicateCount == 1 ? " duplicate" : " duplicates");
        }
        if (failureCount > 0) {
            summary.append(", ").append(failureCount).append(failureCount == 1 ? " failure" : " failures");
        }
        summary.append(".");
        importMessageLabel.setText(summary.toString());

        // refreshes the rest of the UI only when at least one file made it through so the workspace state matches reality
        if (successCount > 0 && lastImported != null) {
            updateSummary(lastImported);
            setWorkspaceEnabled(true);
            sentenceDraftArea.clear();
            generateOutputArea.clear();
            autocompleteCommittedWordField.clear();
            resetAutocompleteSuggestions();
            clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            refreshReports();
        }
    }
    // End of Code by Shriram

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
            log("Generated a new draft using " + algorithmBox.getValue() + " mode.");
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

    private void appendWordToDraft(String word, boolean appendTrailingSpace) {
        // Suggestions are appended with whitespace normalization so the draft stays readable.
        if (word == null || word.isBlank()) {
            return;
        }

        // sammy 4/14: rebuilds the draft text in one helper so tab can optionally leave a trailing space behind for the next ghost suggestion.
        sentenceDraftArea.setText(buildDraftAfterAppendingWord(sentenceDraftArea.getText(), word, appendTrailingSpace));
        // sammy 3/30: moves the caret to the end so the draft still feels correct if the user wants to keep typing.
        sentenceDraftArea.positionCaret(sentenceDraftArea.getText().length());
        log("Added suggestion to draft: " + word);
    }

    // sammy 4/14: keeps the "append accepted suggestion" formatting in one simple helper so click and tab can differ only by trailing-space behavior.
    static String buildDraftAfterAppendingWord(String currentDraft, String word, boolean appendTrailingSpace) {
        // sammy 4/14: treats null inputs like blanks so this helper stays safe during ui setup and tests.
        String safeDraft = currentDraft == null ? "" : currentDraft.trim();
        // sammy 4/14: normalizes the accepted word before rebuilding the draft text.
        String safeWord = word == null ? "" : word.trim();
        // sammy 4/14: returns the original draft unchanged if there is no usable word to append.
        if (safeWord.isBlank()) {
            return safeDraft;
        }
        // sammy 4/14: joins the accepted word onto the draft with one normal separator space when the draft already has content.
        String updatedDraft = safeDraft.isBlank() ? safeWord : safeDraft + " " + safeWord;
        // sammy 4/14: adds one real trailing space only for the tab flow so ghost text can appear again immediately.
        return appendTrailingSpace ? updatedDraft + " " : updatedDraft;
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
        } catch (SQLException exception) {
            clearDraftSuggestionPreview("Live suggestions failed: " + exception.getMessage());
        }
    }

    // sammy 3/30: renders each suggestion as a plain row and ignores clicks on empty space inside the list view.
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

                setText(item);
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
        // sammy 4/14: keeps click-based suggestion acceptance using the normal no-trailing-space behavior.
        applySuggestionSelection(selectedWord, false);
    }

    // sammy 4/14: keeps the acceptance flow in one place while letting tab choose whether it should leave a trailing space behind.
    private void applySuggestionSelection(String selectedWord, boolean appendTrailingSpace) {
        if (selectedWord == null || selectedWord.isBlank()) {
            return;
        }

        // sammy 4/7: pauses the draft preview, writes the accepted word, syncs the autocomplete field,
        // and then asks autocomplete for the next word so the ghost text can keep moving forward.
        suppressDraftSuggestionPreview = true;
        // sammy 4/14: lets tab acceptance add a real trailing space while keeping the click flow unchanged.
        appendWordToDraft(selectedWord, appendTrailingSpace);
        suppressDraftSuggestionPreview = false;
        autocompleteCommittedWordField.setText(selectedWord);
        suggestionsView.getSelectionModel().clearSelection();
        if (appendTrailingSpace) {
            // sammy 4/14: waits to load the next ghost suggestion until JavaFX finishes moving the caret after tab inserts the word and space.
            queueSuggestionRefreshAfterTabAcceptance(selectedWord);
            return;
        }
        requestSuggestions(selectedWord, ' ', suggestionLimitSpinner.getValue(), false);
    }

    // sammy 3/30: updates the list, empty-state text, and status label together so autocomplete feedback stays consistent.
    private void renderAutocompleteState(AutocompleteViewState state) {
        suggestionsView.getItems().setAll(state.suggestions());
        suggestionsView.getSelectionModel().clearSelection();
        autocompleteStatusValue.setText(state.feedbackMessage());
        // sammy 4/7: mirrors the current autocomplete result into the draft hint so the same top suggestion can be accepted with tab.
        updateDraftSuggestionPreview(state);

        if (state.hasSuggestions()) {
            autocompletePlaceholderLabel.setText("Suggestions are ready. Click one to keep building your draft.");
            suggestionsView.scrollTo(0);
            return;
        }

        autocompletePlaceholderLabel.setText(
            state.feedbackMessage().isBlank() ? DEFAULT_AUTOCOMPLETE_MESSAGE : state.feedbackMessage()
        );
    }

    // sammy 4/7: keeps the draft-side tab hint matched to whatever autocomplete most recently found.
    private void updateDraftSuggestionPreview(AutocompleteViewState state) {
        // sammy 4/7: shows the top suggestion as inline ghost text when there is one and hides it when there is not.
        if (state == null || !state.hasSuggestions() || state.suggestions().isEmpty()) {
            clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            return;
        }

        // sammy 4/7: saves the current best suggestion, formats it the way it would be inserted, and then repositions it.
        //Code by Archisha Sasson
        draftTopSuggestion = selectDraftGhostSuggestion(state);
        //End of Code by Archisha Sasson
        if (draftTopSuggestion == null || draftTopSuggestion.isBlank()) {
            clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
            return;
        }

        //Code by Archisha Sasson
        displayedDraftSuggestion = draftTopSuggestion;
        draftSuggestionValue.setText(buildDraftGhostSuggestion(sentenceDraftArea.getText(), displayedDraftSuggestion));
        draftHelpValue.setText("Tab adds: " + displayedDraftSuggestion);
        //End of Code by Archisha Sasson
        // sammy 4/14: only turns on the ghost label when the caret is at the end of the draft.
        //Code by Archisha Sasson
        updateDraftSuggestionVisibility();
        //End of Code by Archisha Sasson
        draftSuggestionValue.applyCss();
        draftSuggestionValue.autosize();
    }

    //Code by Archisha Sasson
    static String selectDraftGhostSuggestion(AutocompleteViewState state) {
        if (state == null || !state.hasSuggestions() || state.suggestions().isEmpty()) {
            return "";
        }
        String topSuggestion = state.suggestions().get(0);
        return topSuggestion == null ? "" : topSuggestion;
    }
    //End of Code by Archisha Sasson

    //Code by Archisha Sasson
    private void updateDraftSuggestionVisibility() {
        if (!shouldShowDraftGhostSuggestion()) {
            draftSuggestionValue.setVisible(false);
            return;
        }

        draftSuggestionValue.setText(buildDraftGhostSuggestion(sentenceDraftArea.getText(), displayedDraftSuggestion));
        draftSuggestionValue.applyCss();
        draftSuggestionValue.autosize();
        draftSuggestionValue.setVisible(true);
        draftSuggestionValue.toFront();
        queueDraftSuggestionPosition();
    }
    //End of Code by Archisha Sasson

    //Code by Archisha Sasson
    private void queueDraftSuggestionPosition() {
        Platform.runLater(this::positionDraftSuggestionNearCaret);
    }

    private void positionDraftSuggestionNearCaret() {
        if (!shouldShowDraftGhostSuggestion() || draftSuggestionOverlay == null) {
            draftSuggestionValue.setVisible(false);
            return;
        }

        Bounds caretBounds = getDraftCaretBoundsInOverlay();
        if (caretBounds == null) {
            draftSuggestionValue.setVisible(false);
            return;
        }

        draftSuggestionValue.setText(buildDraftGhostSuggestion(sentenceDraftArea.getText(), displayedDraftSuggestion));
        draftSuggestionValue.applyCss();
        draftSuggestionValue.autosize();

        double ghostWidth = draftSuggestionValue.prefWidth(-1);
        double ghostHeight = draftSuggestionValue.prefHeight(-1);
        //Code by Archisha Sasson
        DraftGhostPlacement placement = calculateDraftGhostPlacement(
            caretBounds.getMaxX(),
            caretBounds.getMinY(),
            caretBounds.getHeight(),
            ghostWidth,
            ghostHeight,
            draftSuggestionOverlay.getWidth(),
            draftSuggestionOverlay.getHeight()
        );
        //End of Code by Archisha Sasson
        if (placement == null) {
            draftSuggestionValue.setVisible(false);
            return;
        }

        draftSuggestionValue.relocate(placement.x(), placement.y());
        draftSuggestionValue.setVisible(true);
        draftSuggestionValue.toFront();
    }

    //Code by Archisha Sasson
    static DraftGhostPlacement calculateDraftGhostPlacement(
        double caretMaxX,
        double caretMinY,
        double caretHeight,
        double ghostWidth,
        double ghostHeight,
        double overlayWidth,
        double overlayHeight
    ) {
        double contentRight = Math.max(0, overlayWidth - DRAFT_GHOST_MARGIN);
        double contentBottom = Math.max(0, overlayHeight - DRAFT_GHOST_MARGIN);
        double sameLineX = Math.max(DRAFT_GHOST_MARGIN, caretMaxX + DRAFT_GHOST_CARET_GAP);
        double sameLineY = Math.max(0, caretMinY);

        if (contentRight <= 0 || contentBottom <= 0) {
            return null;
        }
        if (sameLineX + ghostWidth <= contentRight && sameLineY + ghostHeight <= contentBottom) {
            return new DraftGhostPlacement(sameLineX, sameLineY);
        }

        return null;
    }

    record DraftGhostPlacement(double x, double y) {}
    //End of Code by Archisha Sasson

    private Bounds getDraftCaretBoundsInOverlay() {
        if (sentenceDraftArea.getScene() == null
            || draftSuggestionOverlay == null
            || !(sentenceDraftArea.getSkin() instanceof TextAreaSkin textAreaSkin)) {
            return null;
        }

        //Code by Archisha Sasson
        int caretPosition = sentenceDraftArea.getCaretPosition();
        if (caretPosition <= 0) {
            return null;
        }

        Rectangle2D characterBounds = textAreaSkin.getCharacterBounds(caretPosition - 1);
        if (characterBounds == null) {
            return null;
        }

        Bounds charBounds = sentenceDraftArea.localToScene(new BoundingBox(
            characterBounds.getMinX(),
            characterBounds.getMinY(),
            characterBounds.getWidth(),
            characterBounds.getHeight()
        ));
        return draftSuggestionOverlay.sceneToLocal(charBounds);
        //End of Code by Archisha Sasson
    }
    //End of Code by Archisha Sasson

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
        //Code by Archisha Sasson
        displayedDraftSuggestion = "";
        //End of Code by Archisha Sasson
        draftSuggestionValue.setText("");
        draftSuggestionValue.setVisible(false);

        if (message != null && !message.isBlank()) {
            draftHelpValue.setText(message);
        }
    }

    // sammy 4/14: reloads suggestions after tab acceptance only after JavaFX finishes updating the caret for the new trailing space.
    private void queueSuggestionRefreshAfterTabAcceptance(String selectedWord) {
        // sammy 4/14: hides the old ghost preview during the tab transition so stale positioning does not flash on screen.
        clearDraftSuggestionPreview(DEFAULT_DRAFT_SUGGESTION_MESSAGE);
        // sammy 4/14: lets the text area finish rewriting its text and caret before the next suggestion is rendered.
        Platform.runLater(() -> {
            // sammy 4/14: restores the caret to the true text end in case the tab rewrite briefly leaves it on the old accepted word.
            sentenceDraftArea.positionCaret(sentenceDraftArea.getText().length());
            // sammy 4/14: asks JavaFX to refresh the control layout so the skin recalculates the caret bounds for the new trailing space.
            sentenceDraftArea.requestLayout();
            // sammy 4/14: waits one more pulse and then requests the next suggestion so the new ghost text uses the updated caret bounds.
            Platform.runLater(() -> requestSuggestions(
                selectedWord,
                ' ',
                suggestionLimitSpinner == null ? 5 : suggestionLimitSpinner.getValue(),
                false
            ));
        });
    }

    // sammy 4/14: keeps the "show ghost text" rule in one place so the draft preview and tab shortcut stay consistent.
    private boolean isCaretAtDraftEnd() {
        // sammy 4/14: compares the live caret position against the full draft length instead of guessing from line layout.
        return isCaretAtDraftEnd(sentenceDraftArea.getText(), sentenceDraftArea.getCaretPosition());
    }

    // sammy 4/14: gives the caret-at-end rule a simple helper so it is easy to test without spinning up the full app ui.
    static boolean isCaretAtDraftEnd(String draftText, int caretPosition) {
        // sammy 4/14: treats null text like an empty draft so the check stays safe during setup paths.
        String safeDraftText = draftText == null ? "" : draftText;
        // sammy 4/14: clamps the caret into the valid text range before deciding whether it is truly at the end.
        int safeCaretPosition = Math.max(0, Math.min(caretPosition, safeDraftText.length()));
        // sammy 4/14: returns true only when the caret is exactly at the last character boundary in the draft.
        return safeCaretPosition == safeDraftText.length();
    }

    // sammy 4/14: keeps ghost text hidden unless the user is at the end of the draft.
    private boolean shouldShowDraftGhostSuggestion() {
        // sammy 4/14: reuses one helper so visibility stays matched between refresh-time and live caret movement.
        //Code by Archisha Sasson
        return shouldShowDraftGhostSuggestion(sentenceDraftArea.getText(), sentenceDraftArea.getCaretPosition(), displayedDraftSuggestion);
        //End of Code by Archisha Sasson
    }

    // sammy 4/14: gives the ghost-visibility rule a simple testable helper without needing the full JavaFX app to run.
    static boolean shouldShowDraftGhostSuggestion(String draftText, int caretPosition, String suggestion) {
        // sammy 4/14: requires a real suggestion first so the ghost label never appears empty.
        if (suggestion == null || suggestion.isBlank()) {
            return false;
        }
        // sammy 4/14: only allows the ghost text when the caret is truly at the draft end.
        if (!isCaretAtDraftEnd(draftText, caretPosition)) {
            return false;
        }
        //Code by Archisha Sasson
        return isAtLastWordBoundary(draftText, caretPosition);
        //End of Code by Archisha Sasson
    }

    //Code by Archisha Sasson
    static boolean isAtLastWordBoundary(String draftText, int caretPosition) {
        String safeDraftText = draftText == null ? "" : draftText;
        if (safeDraftText.isBlank() || caretPosition != safeDraftText.length()) {
            return false;
        }

        char lastChar = safeDraftText.charAt(safeDraftText.length() - 1);
        if (!Character.isWhitespace(lastChar)) {
            return true;
        }

        int lastWordEnd = safeDraftText.length() - 2;
        return lastChar == ' ' && lastWordEnd >= 0 && !Character.isWhitespace(safeDraftText.charAt(lastWordEnd));
    }
    //End of Code by Archisha Sasson

    // sammy 4/7: formats the inline suggestion the same way tab accept would add it into the draft.
    static String buildDraftGhostSuggestion(String currentDraft, String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return "";
        }
        //Code by Archisha Sasson
        // Show only the inline ghost word that Tab will add, not the whole draft sentence.
        String safeDraft = currentDraft == null ? "" : currentDraft;
        String safeSuggestion = suggestion.trim();
        if (safeDraft.isBlank() || Character.isWhitespace(safeDraft.charAt(safeDraft.length() - 1))) {
            return safeSuggestion;
        }
        return " " + safeSuggestion;
        //End of Code by Archisha Sasson
    }

    private void refreshReports() {
        // Refresh both reporting surfaces together so they always describe the same preview state.
        try {
            wordRows.setAll(reportsController.listWords(
                reportSortBox == null ? WordReportSort.ALPHABETICAL : reportSortBox.getValue(),
                reportWordLimitSpinner == null || reportWordLimitSpinner.getValue() == null
                    ? 50
                    : reportWordLimitSpinner.getValue().intValue(),
                reportSearchField == null ? "" : reportSearchField.getText(),
                reportSecondWordField == null ? "" : reportSecondWordField.getText()
            ));
            sentenceRows.setAll(reportsController.listGeneratedSentences(
                duplicatesOnlyCheckBox != null && duplicatesOnlyCheckBox.isSelected(),
                reportSentenceLimitSpinner == null || reportSentenceLimitSpinner.getValue() == null
                    ? 25
                    : reportSentenceLimitSpinner.getValue().intValue()
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
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #5b1f2a;");
        label.setWrapText(true);
        return label;
    }

    private static Label titledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6f1d2a;");
        return label;
    }

    private static String cardStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + ";" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(111,29,42,0.14);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;";
    }

    private boolean databaseHasWorkspaceData() {
        try (Connection conn = WordDb.openConnection()) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM next_word LIMIT 1");
                 java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException exception) {
            return false;
        }
    }

    private void loadStartupSummaryFromDatabase() {
        try (Connection conn = WordDb.openConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                 "SELECT file_name, word_count, sentence_count "
                     + "FROM files "
                     + "ORDER BY imported_at DESC, file_id DESC "
                     + "LIMIT 1"
             );
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return;
            }

            ParseResult result = new ParseResult();
            result.setFileName(rs.getString("file_name"));
            result.setTotalWords(rs.getInt("word_count"));
            result.setTotalSentences(rs.getInt("sentence_count"));
            result.setTotalParagraphs(0);
            updateSummary(result);
        } catch (SQLException exception) {
            log("Startup summary load failed: " + exception.getMessage());
        }
    }
}
