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

public final class FxRepeatTabContentController {
    @FXML
    private TextField repeatInputField;
    @FXML
    private Button repeatBrowseButton;
    @FXML
    private Spinner<Integer> repeatSpinner;
    @FXML
    private TextField repeatOutputField;
    @FXML
    private Button repeatButton;
    @FXML
    private ProgressIndicator repeatProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        repeatSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 2));
        repeatSpinner.setEditable(true);

        repeatBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                repeatInputField.setText(selected.getAbsolutePath());
                updateSuggestion(repeatOutputField, controller, selected.toPath(), repeatSpinner.getValue());
            }
        });

        repeatInputField.textProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestionIfNeeded(repeatOutputField, controller, newVal, repeatSpinner.getValue()));

        repeatSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestionIfNeeded(repeatOutputField, controller, repeatInputField.getText(), newVal));

        repeatButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.repeatPdf(repeatInputField.getText(), repeatSpinner.getValue(),
                            repeatOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(repeatButton, repeatProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Operazione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'elaborazione."), owner));
            new Thread(task, "repeat-task").start();
        });
    }

    private static void updateSuggestionIfNeeded(TextField outputField, PdfUtilityFxController controller, String input,
            int repetitions) {
        if (!outputField.getText().trim().isEmpty()) {
            return;
        }
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(trimmed);
            updateSuggestion(outputField, controller, path, repetitions);
        } catch (Exception ignored) {
            // wait for valid path
        }
    }

    private static void updateSuggestion(TextField outputField, PdfUtilityFxController controller, Path path,
            int repetitions) {
        if (outputField.getText().trim().isEmpty()) {
            outputField.setText(controller.buildDefaultRepeatedName(path, repetitions));
        }
    }
}
