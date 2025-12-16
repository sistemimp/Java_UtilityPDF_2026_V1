package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxCsvToExcelTabContentController {
    @FXML
    private TextField csvToExcelCsvField;
    @FXML
    private Button csvToExcelCsvBrowse;
    @FXML
    private TextField csvToExcelOutputField;
    @FXML
    private Button csvToExcelOutputBrowse;
    @FXML
    private Button csvToExcelButton;
    @FXML
    private ProgressIndicator csvToExcelProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        csvToExcelCsvBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                csvToExcelCsvField.setText(selected.getAbsolutePath());
            }
        });

        csvToExcelCsvField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!csvToExcelOutputField.getText().trim().isEmpty()) {
                return;
            }
            String suggestion = controller.buildDefaultExcelName(newVal);
            if (!suggestion.isEmpty()) {
                csvToExcelOutputField.setText(suggestion);
            }
        });

        csvToExcelOutputBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            String suggestion = csvToExcelOutputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.buildDefaultExcelName(csvToExcelCsvField.getText());
            }
            if (!suggestion.isEmpty()) {
                try {
                    Path path = Paths.get(suggestion);
                    chooser.setInitialFileName(path.getFileName().toString());
                    Path parent = path.getParent();
                    if (parent != null && Files.isDirectory(parent)) {
                        chooser.setInitialDirectory(parent.toFile());
                    }
                } catch (Exception ignored) {
                    // ignore invalid suggestion
                }
            }
            File selected = chooser.showSaveDialog(owner);
            if (selected != null) {
                Path resolved = ensureExtension(selected.toPath(), ".xlsx");
                csvToExcelOutputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        csvToExcelButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.convertCsvToExcel(csvToExcelCsvField.getText(), csvToExcelOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(csvToExcelButton, csvToExcelProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Conversione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante la conversione."), owner));
            new Thread(task, "csv-to-excel-task").start();
        });
    }

    private static Path ensureExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        if (!filename.toLowerCase().endsWith(extension)) {
            path = path.resolveSibling(filename + extension);
        }
        return path;
    }
}
