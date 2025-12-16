package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FxOptimizeTabContentController {
    @FXML
    private TextField optimizeInputField;
    @FXML
    private Button optimizeBrowseButton;
    @FXML
    private TextField optimizeOutputField;
    @FXML
    private Button optimizeButton;
    @FXML
    private ProgressIndicator optimizeProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        optimizeBrowseButton.setOnAction(event -> {
            if (chooseDirectory(optimizeInputField, optimizeOutputField, owner)) {
                return;
            }
            chooseFile(optimizeInputField, optimizeOutputField, controller, owner);
        });

        optimizeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.optimize(optimizeInputField.getText(), optimizeOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(optimizeButton, optimizeProgress, task);
            task.setOnSucceeded(e -> showSuccess(task.getValue()));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'ottimizzazione.", true));
            new Thread(task, "optimize-task").start();
        });
    }

    private void showSuccess(Path result) {
        if (result == null) {
            return;
        }
        String summary = Files.isDirectory(result) ? "Cartella creata: " : "File creato: ";
        FxDialogUtils.showInformation("Successo", "Ottimizzazione completata!\n" + summary + result, owner);
    }

    private static boolean chooseDirectory(TextField inputField, TextField outputField, Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella");
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            inputField.setText(selected.getAbsolutePath());
            outputField.setText("");
            return true;
        }
        return false;
    }

    private static void chooseFile(TextField inputField, TextField outputField, PdfUtilityFxController controller,
            Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            inputField.setText(selected.getAbsolutePath());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(controller.buildDefaultOptimizedName(selected.toPath()));
            }
        }
    }
}
