package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxConditionalBlankTabContentController {
    @FXML
    private TextField conditionalInputField;
    @FXML
    private Button conditionalBrowseButton;
    @FXML
    private TextField conditionalPhraseField;
    @FXML
    private CheckBox conditionalCaseSensitive;
    @FXML
    private CheckBox conditionalOddPageOnly;
    @FXML
    private TextField conditionalOutputField;
    @FXML
    private Button conditionalProcessButton;
    @FXML
    private ProgressIndicator conditionalProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        conditionalBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                conditionalInputField.setText(selected.getAbsolutePath());
                updateSuggestion(conditionalOutputField, controller, selected.toPath(),
                        conditionalPhraseField.getText());
            }
        });

        conditionalInputField.textProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestionIfNeeded(conditionalOutputField, controller, newVal, conditionalPhraseField.getText()));
        conditionalPhraseField.textProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestionIfNeeded(conditionalOutputField, controller, conditionalInputField.getText(), newVal));

        conditionalProcessButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.insertBlankAfterPhrase(conditionalInputField.getText(),
                            conditionalPhraseField.getText(), conditionalCaseSensitive.isSelected(),
                            conditionalOddPageOnly.isSelected(), conditionalOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(conditionalProcessButton, conditionalProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Operazione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'elaborazione.", true));
            new Thread(task, "conditional-blank-task").start();
        });
    }

    private static void updateSuggestionIfNeeded(TextField outputField, PdfUtilityFxController controller, String input,
            String phrase) {
        if (!outputField.getText().trim().isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(input == null ? "" : input.trim());
            updateSuggestion(outputField, controller, path, phrase);
        } catch (Exception ignored) {
            // wait for valid path
        }
    }

    private static void updateSuggestion(TextField outputField, PdfUtilityFxController controller, Path path,
            String phrase) {
        if (controller == null || outputField == null || path == null) {
            return;
        }
        if (outputField.getText().trim().isEmpty()) {
            outputField.setText(controller.buildDefaultBlankAfterPhraseName(path, phrase));
        }
    }
}
