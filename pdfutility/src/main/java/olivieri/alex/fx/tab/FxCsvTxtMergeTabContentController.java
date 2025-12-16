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

public final class FxCsvTxtMergeTabContentController {
    @FXML
    private ListView<String> csvTxtFileList;
    @FXML
    private Button csvTxtAddButton;
    @FXML
    private Button csvTxtRemoveButton;
    @FXML
    private TextField csvTxtOutputField;
    @FXML
    private Button csvTxtOutputBrowse;
    @FXML
    private Button csvTxtButton;
    @FXML
    private ProgressIndicator csvTxtProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private String lastSuggestion = "";

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        csvTxtFileList.setItems(FXCollections.observableArrayList());

        Runnable updateSuggestion = () -> {
            if (csvTxtFileList.getItems().isEmpty()) {
                lastSuggestion = "";
                return;
            }
            String current = csvTxtOutputField.getText().trim();
            try {
                Path candidate = Paths.get(csvTxtFileList.getItems().get(0));
                String suggestion = controller.buildDefaultMergedTextName(candidate);
                if (suggestion.isEmpty()) {
                    return;
                }
                if (current.isEmpty() || current.equals(lastSuggestion)) {
                    csvTxtOutputField.setText(suggestion);
                    lastSuggestion = suggestion;
                }
            } catch (Exception ignored) {
                // ignore invalid first entry
            }
        };

        csvTxtAddButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV/TXT");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV o TXT", "*.csv", "*.txt"));
            List<File> selected = chooser.showOpenMultipleDialog(owner);
            if (selected != null) {
                for (File file : selected) {
                    if (file != null) {
                        csvTxtFileList.getItems().add(file.getAbsolutePath());
                    }
                }
                updateSuggestion.run();
            }
        });

        csvTxtRemoveButton.setOnAction(event -> {
            List<String> selected = new ArrayList<>(csvTxtFileList.getSelectionModel().getSelectedItems());
            csvTxtFileList.getItems().removeAll(selected);
            updateSuggestion.run();
        });

        csvTxtOutputBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file di output");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV o TXT", "*.csv", "*.txt"));
            String suggestion = csvTxtOutputField.getText().trim();
            if (suggestion.isEmpty() && !csvTxtFileList.getItems().isEmpty()) {
                try {
                    suggestion = controller.buildDefaultMergedTextName(Paths.get(csvTxtFileList.getItems().get(0)));
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
                if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
                    resolved = selected.toPath();
                } else {
                    String extension = ".csv";
                    resolved = selected.toPath().resolveSibling(filename + extension);
                }
                csvTxtOutputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        csvTxtButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    List<String> inputs = new ArrayList<>(csvTxtFileList.getItems());
                    return controller.mergeCsvTxt(inputs, csvTxtOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(csvTxtButton, csvTxtProgress, task);
            task.setOnSucceeded(e -> {
                Path result = task.getValue();
                lastSuggestion = result.toAbsolutePath().toString();
                FxDialogUtils.showInformation("Successo",
                        "Unione completata!\nFile creato: " + result.toAbsolutePath(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'unione."), owner));
            new Thread(task, "csv-txt-merge-task").start();
        });
    }
}
