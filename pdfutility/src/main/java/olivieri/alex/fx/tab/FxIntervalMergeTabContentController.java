package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfMergeService.BatchMergeResult;
import olivieri.alex.util.PdfMergeService.RotationMode;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxIntervalMergeTabContentController {
    @FXML
    private TextField intervalDirectoryField;
    @FXML
    private Button intervalBrowseButton;
    @FXML
    private TextField intervalOutputDirectoryField;
    @FXML
    private Button intervalOutputBrowseButton;
    @FXML
    private Spinner<Integer> intervalGroupSizeSpinner;
    @FXML
    private TextField intervalPrefixField;
    @FXML
    private CheckBox intervalRotateClockwise;
    @FXML
    private CheckBox intervalRotateCounterClockwise;
    @FXML
    private ProgressIndicator intervalProgress;
    @FXML
    private Button intervalMergeButton;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        intervalGroupSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 500));
        intervalGroupSizeSpinner.setEditable(true);

        intervalBrowseButton.setOnAction(event -> chooseSourceDirectory());
        intervalOutputBrowseButton.setOnAction(event -> chooseOutputDirectory());
        intervalDirectoryField.textProperty().addListener((obs, oldValue, newValue) -> updateOutputSuggestionIfNeeded());
        setupRotationToggle();

        intervalMergeButton.setOnAction(event -> startBatchMergeTask());
    }

    private void chooseSourceDirectory() {
        if (owner == null) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella PDF");
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            intervalDirectoryField.setText(selected.getAbsolutePath());
            updateOutputSuggestionIfNeeded();
        }
    }

    private void chooseOutputDirectory() {
        if (owner == null) {
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella output gruppi");
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            intervalOutputDirectoryField.setText(selected.getAbsolutePath());
        }
    }

    private void startBatchMergeTask() {
        Task<BatchMergeResult> task = new Task<>() {
            @Override
            protected BatchMergeResult call() throws Exception {
                return controller.mergeDirectoryInBatches(intervalDirectoryField.getText(),
                        intervalOutputDirectoryField.getText(), getGroupSize(), intervalPrefixField.getText(),
                        extractRotationMode());
            }
        };
        FxTabControllerSupport.bindUiState(intervalMergeButton, intervalProgress, task);
        task.setOnSucceeded(event -> showSuccess(task.getValue()));
        task.setOnFailed(event -> FxTabControllerSupport.showFailure(owner, task.getException(),
                "Errore durante il merge a blocchi.", true));
        new Thread(task, "interval-merge-task").start();
    }

    private int getGroupSize() {
        if (intervalGroupSizeSpinner == null) {
            return 1;
        }
        Integer spinnerValue = intervalGroupSizeSpinner.getValue();
        if (spinnerValue != null) {
            return spinnerValue;
        }
        String editorText = intervalGroupSizeSpinner.getEditor() != null ? intervalGroupSizeSpinner.getEditor().getText()
                : "";
        try {
            return Integer.parseInt(editorText.trim());
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void setupRotationToggle() {
        if (intervalRotateClockwise != null && intervalRotateCounterClockwise != null) {
            intervalRotateClockwise.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    intervalRotateCounterClockwise.setSelected(false);
                }
            });
            intervalRotateCounterClockwise.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    intervalRotateClockwise.setSelected(false);
                }
            });
        }
    }

    private RotationMode extractRotationMode() {
        if (intervalRotateClockwise != null && intervalRotateClockwise.isSelected()) {
            return RotationMode.CLOCKWISE;
        }
        if (intervalRotateCounterClockwise != null && intervalRotateCounterClockwise.isSelected()) {
            return RotationMode.COUNTERCLOCKWISE;
        }
        return RotationMode.NONE;
    }

    private void updateOutputSuggestionIfNeeded() {
        if (controller == null || intervalOutputDirectoryField == null || intervalDirectoryField == null) {
            return;
        }
        if (!intervalOutputDirectoryField.getText().trim().isEmpty()) {
            return;
        }
        String sourceText = intervalDirectoryField.getText();
        if (sourceText == null || sourceText.trim().isEmpty()) {
            return;
        }
        try {
            Path sourcePath = Paths.get(sourceText.trim());
            String folderName = controller.buildDefaultBatchMergeDirectoryName(sourcePath);
            intervalOutputDirectoryField.setText(sourcePath.resolve(folderName).toString());
        } catch (Exception ignored) {
            // Ignore invalid source path while user is typing.
        }
    }

    private void showSuccess(BatchMergeResult result) {
        String message = "Merge a blocchi completato!\nPDF sorgente: " + result.getSourcePdfCount()
                + "\nGruppi creati: " + result.getMergedGroupCount() + "\nDimensione gruppo: " + result.getGroupSize()
                + "\nCartella output: " + result.getOutputDirectory();
        FxDialogUtils.showInformation("Successo", message, owner);
    }
}
