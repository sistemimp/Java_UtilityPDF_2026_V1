package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfMergeService.RotationMode;

import java.io.File;
import java.nio.file.Path;

public final class FxMergeTabContentController {
    @FXML
    private TextField mergeDirectoryField;
    @FXML
    private Button mergeBrowseButton;
    @FXML
    private TextField mergeOutputField;
    @FXML
    private Button mergeButton;
    @FXML
    private ProgressIndicator mergeProgress;
    @FXML
    private CheckBox rotateClockwise;
    @FXML
    private CheckBox rotateCounterClockwise;
    @FXML
    private CheckBox forceA4CheckBox;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        mergeBrowseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                mergeDirectoryField.setText(selected.getAbsolutePath());
            }
        });
        setupRotationToggle();
        mergeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.mergeDirectory(mergeDirectoryField.getText(), mergeOutputField.getText(),
                            extractRotationMode(), isForceA4Enabled());
                }
            };
            bindUiState(mergeButton, mergeProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Unione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'unione."), owner));
            new Thread(task, "merge-task").start();
        });
    }

    private static void bindUiState(Button actionButton, ProgressIndicator indicator, Task<?> task) {
        actionButton.disableProperty().bind(task.runningProperty());
        indicator.visibleProperty().bind(task.runningProperty());
    }

    private void setupRotationToggle() {
        if (rotateClockwise != null && rotateCounterClockwise != null) {
            rotateClockwise.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    rotateCounterClockwise.setSelected(false);
                }
            });
            rotateCounterClockwise.selectedProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue) {
                    rotateClockwise.setSelected(false);
                }
            });
        }
    }

    private RotationMode extractRotationMode() {
        if (rotateClockwise != null && rotateClockwise.isSelected()) {
            return RotationMode.CLOCKWISE;
        }
        if (rotateCounterClockwise != null && rotateCounterClockwise.isSelected()) {
            return RotationMode.COUNTERCLOCKWISE;
        }
        return RotationMode.NONE;
    }

    private boolean isForceA4Enabled() {
        return forceA4CheckBox != null && forceA4CheckBox.isSelected();
    }

    private static String getRootCauseMessage(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null && !message.isEmpty() ? message : fallback;
    }
}
