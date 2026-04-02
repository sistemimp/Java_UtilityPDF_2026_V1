package olivieri.alex.fx.tab;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FxDuMergeTabContentController {
    @FXML
    private ListView<String> duFileList;
    @FXML
    private Button duAddButton;
    @FXML
    private Button duRemoveButton;
    @FXML
    private TextField duOutputField;
    @FXML
    private Button duOutputBrowse;
    @FXML
    private Button duMergeButton;
    @FXML
    private ProgressIndicator duProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private String lastSuggestion = "";

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        duFileList.setItems(FXCollections.observableArrayList());

        Runnable updateSuggestion = () -> {
            if (duFileList.getItems().isEmpty()) {
                lastSuggestion = "";
                return;
            }
            String current = duOutputField.getText().trim();
            try {
                Path candidate = Paths.get(duFileList.getItems().get(0));
                String suggestion = controller.buildDefaultMergedDuName(candidate);
                if (suggestion.isEmpty()) {
                    return;
                }
                if (current.isEmpty() || current.equals(lastSuggestion)) {
                    duOutputField.setText(suggestion);
                    lastSuggestion = suggestion;
                }
            } catch (Exception ignored) {
                // ignore invalid first entry
            }
        };

        duAddButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file DU");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File DU", "*.DU", "*.du"));
            List<File> selected = chooser.showOpenMultipleDialog(owner);
            if (selected != null) {
                for (File file : selected) {
                    if (file != null) {
                        duFileList.getItems().add(file.getAbsolutePath());
                    }
                }
                updateSuggestion.run();
            }
        });

        duRemoveButton.setOnAction(event -> {
            List<String> selected = new ArrayList<>(duFileList.getSelectionModel().getSelectedItems());
            duFileList.getItems().removeAll(selected);
            updateSuggestion.run();
        });

        duOutputBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file DU di output");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("File DU", "*.DU", "*.du"));
            String suggestion = duOutputField.getText().trim();
            if (suggestion.isEmpty() && !duFileList.getItems().isEmpty()) {
                try {
                    suggestion = controller.buildDefaultMergedDuName(Paths.get(duFileList.getItems().get(0)));
                } catch (Exception ignored) {
                    // ignore
                }
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
                String filename = selected.getName();
                String lower = filename.toLowerCase();
                Path resolved;
                if (lower.endsWith(".du")) {
                    resolved = selected.toPath();
                } else {
                    resolved = selected.toPath().resolveSibling(filename + ".DU");
                }
                duOutputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        duMergeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    List<String> inputs = new ArrayList<>(duFileList.getItems());
                    return controller.mergeDuFiles(inputs, duOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(duMergeButton, duProgress, task);
            task.setOnSucceeded(e -> {
                Path result = task.getValue();
                lastSuggestion = result.toAbsolutePath().toString();
                FxDialogUtils.showInformation("Successo",
                        "Unione DU completata!\nFile creato: " + result.toAbsolutePath(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'unione DU."),
                    owner));
            new Thread(task, "du-merge-task").start();
        });
    }
}
