package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public final class FxAlternatingMixTabContentController {
    @FXML
    private TextField firstInputField;
    @FXML
    private Button firstBrowseButton;
    @FXML
    private TextField secondInputField;
    @FXML
    private Button secondBrowseButton;
    @FXML
    private Spinner<Integer> firstChunkSpinner;
    @FXML
    private Spinner<Integer> secondChunkSpinner;
    @FXML
    private TextField outputField;
    @FXML
    private Button outputBrowseButton;
    @FXML
    private Button mixButton;
    @FXML
    private ProgressIndicator mixProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        firstChunkSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        secondChunkSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        firstChunkSpinner.setEditable(true);
        secondChunkSpinner.setEditable(true);

        firstBrowseButton.setOnAction(event -> chooseFirstPdf());
        secondBrowseButton.setOnAction(event -> chooseSecondPdf());
        outputBrowseButton.setOnAction(event -> chooseOutputFile());

        firstInputField.textProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestionIfNeeded(outputField, controller, newVal));

        mixButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.alternatingMix(firstInputField.getText(), secondInputField.getText(),
                            getSpinnerValue(firstChunkSpinner), getSpinnerValue(secondChunkSpinner),
                            outputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(mixButton, mixProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Miscelazione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante la miscelazione.", true));
            new Thread(task, "alternating-mix-task").start();
        });
    }

    private void chooseFirstPdf() {
        File selected = choosePdfFile("Seleziona il primo PDF");
        if (selected != null) {
            firstInputField.setText(selected.getAbsolutePath());
            updateSuggestionIfNeeded(outputField, controller, selected.getAbsolutePath());
        }
    }

    private void chooseSecondPdf() {
        File selected = choosePdfFile("Seleziona il secondo PDF");
        if (selected != null) {
            secondInputField.setText(selected.getAbsolutePath());
        }
    }

    private File choosePdfFile(String title) {
        if (owner == null) {
            return null;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        return chooser.showOpenDialog(owner);
    }

    private void chooseOutputFile() {
        if (owner == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salva miscelazione alternata");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String suggestion = controller != null ? controller.buildDefaultAlternatingMixName(firstInputField.getText()) : "";
        if (!suggestion.isEmpty()) {
            try {
                Path suggestionPath = Paths.get(suggestion);
                chooser.setInitialFileName(suggestionPath.getFileName().toString());
                Path parent = suggestionPath.getParent();
                if (parent != null && parent.toFile().exists()) {
                    chooser.setInitialDirectory(parent.toFile());
                }
            } catch (Exception ignored) {
                // ignore invalid suggestion
            }
        }
        File selected = chooser.showSaveDialog(owner);
        if (selected != null) {
            outputField.setText(ensurePdfExtension(selected).getAbsolutePath());
        }
    }

    private static File ensurePdfExtension(File file) {
        if (file == null) {
            return null;
        }
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".pdf");
    }

    private static void updateSuggestionIfNeeded(TextField outputField, PdfUtilityFxController controller, String input) {
        if (outputField.getText().trim().isEmpty()) {
            String trimmed = input == null ? "" : input.trim();
            if (!trimmed.isEmpty()) {
                try {
                    Path path = Paths.get(trimmed);
                    outputField.setText(controller.buildDefaultAlternatingMixName(path));
                } catch (Exception ignored) {
                    // wait for valid path
                }
            }
        }
    }

    private static int getSpinnerValue(Spinner<Integer> spinner) {
        Integer value = spinner == null ? null : spinner.getValue();
        return value != null ? value : 1;
    }
}
