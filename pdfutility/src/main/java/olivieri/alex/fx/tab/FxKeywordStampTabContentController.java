package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfKeywordStamper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxKeywordStampTabContentController {
    @FXML
    private TextField keywordStampInputField;
    @FXML
    private Button keywordStampBrowseInput;
    @FXML
    private TextField keywordStampKeywordField;
    @FXML
    private TextField keywordStampTextField;
    @FXML
    private TextField keywordStampOutputField;
    @FXML
    private Button keywordStampBrowseOutput;
    @FXML
    private CheckBox keywordStampCaseSensitive;
    @FXML
    private Spinner<Double> keywordStampXSpinner;
    @FXML
    private Spinner<Double> keywordStampYSpinner;
    @FXML
    private Button keywordStampButton;
    @FXML
    private ProgressIndicator keywordStampProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private String suggestionHolder = "";

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        keywordStampXSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        keywordStampYSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));

        keywordStampBrowseInput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                keywordStampInputField.setText(selected.getAbsolutePath());
                suggestOutput();
            }
        });

        keywordStampKeywordField.textProperty().addListener((obs, oldVal, newVal) -> suggestOutput());
        keywordStampInputField.textProperty().addListener((obs, oldVal, newVal) -> suggestOutput());

        keywordStampBrowseOutput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Salva PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            String suggestion = keywordStampOutputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.buildDefaultKeywordStampName(getPathFrom(keywordStampInputField.getText()),
                        keywordStampKeywordField.getText());
            }
            if (!suggestion.isEmpty()) {
                try {
                    Path path = Paths.get(suggestion);
                    chooser.setInitialFileName(path.getFileName().toString());
                    Path parent = path.getParent();
                    if (parent != null) {
                        chooser.setInitialDirectory(parent.toFile());
                    }
                } catch (Exception ignored) {
                    // ignore invalid suggestion
                }
            }
            File selected = chooser.showSaveDialog(owner);
            if (selected != null) {
                Path resolved = ensureExtension(selected.toPath(), ".pdf");
                keywordStampOutputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        keywordStampButton.setOnAction(event -> {
            Task<PdfKeywordStamper.Result> task = new Task<>() {
                @Override
                protected PdfKeywordStamper.Result call() throws Exception {
                    return controller.stampKeyword(keywordStampInputField.getText(), keywordStampOutputField.getText(),
                            keywordStampKeywordField.getText(), keywordStampTextField.getText(),
                            keywordStampCaseSensitive.isSelected(), keywordStampXSpinner.getValue().floatValue(),
                            keywordStampYSpinner.getValue().floatValue());
                }
            };
            FxTabControllerSupport.bindUiState(keywordStampButton, keywordStampProgress, task);
            task.setOnSucceeded(e -> {
                PdfKeywordStamper.Result result = task.getValue();
                String message = "Timbro completato!\nPagine timbrate: " + result.getStampedPages() + " su "
                        + result.getTotalPages() + "\nFile output: " + result.getOutputFile();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'applicazione del timbro."), owner));
            new Thread(task, "keyword-stamp-task").start();
        });
    }

    private void suggestOutput() {
        String current = keywordStampOutputField.getText().trim();
        if (!current.isEmpty() && !current.equals(suggestionHolder)) {
            return;
        }
        Path path = safePath(keywordStampInputField.getText());
        if (path == null) {
            return;
        }
        String keyword = keywordStampKeywordField.getText().trim();
        if (keyword.isEmpty()) {
            return;
        }
        if (Files.isRegularFile(path)) {
            suggestionHolder = controller.buildDefaultKeywordStampName(path, keyword);
        } else if (Files.isDirectory(path)) {
            suggestionHolder = controller.buildDefaultKeywordStampName(path, keyword);
        } else {
            return;
        }
        if (!suggestionHolder.isEmpty()) {
            keywordStampOutputField.setText(suggestionHolder);
        }
    }

    private static Path getPathFrom(String value) {
        try {
            return Paths.get(value == null ? "" : value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path safePath(String text) {
        try {
            return Paths.get(text == null ? "" : text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path ensureExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        if (!filename.toLowerCase().endsWith(extension)) {
            path = path.resolveSibling(filename + extension);
        }
        return path;
    }
}
