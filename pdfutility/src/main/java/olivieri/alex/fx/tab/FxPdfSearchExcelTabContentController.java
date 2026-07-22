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
import olivieri.alex.util.PdfSearchExcelExtractor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxPdfSearchExcelTabContentController {
    @FXML
    private TextField pdfSearchExcelPdfField;
    @FXML
    private Button pdfSearchExcelPdfBrowse;
    @FXML
    private TextField pdfSearchExcelKeyField;
    @FXML
    private CheckBox pdfSearchExcelCaseSensitive;
    @FXML
    private TextField pdfSearchExcelOutputField;
    @FXML
    private Button pdfSearchExcelOutputBrowse;
    @FXML
    private Button pdfSearchExcelButton;
    @FXML
    private ProgressIndicator pdfSearchExcelProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private String suggestionHolder = "";

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        Runnable updateSuggestion = () -> {
            String currentOutput = pdfSearchExcelOutputField.getText().trim();
            if (!currentOutput.isEmpty() && !currentOutput.equals(suggestionHolder)) {
                return;
            }
            String suggestion = controller.buildDefaultPdfSearchExcelName(pdfSearchExcelPdfField.getText(),
                    pdfSearchExcelKeyField.getText());
            if (!suggestion.isEmpty()) {
                pdfSearchExcelOutputField.setText(suggestion);
                suggestionHolder = suggestion;
            }
        };

        pdfSearchExcelPdfBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                pdfSearchExcelPdfField.setText(selected.getAbsolutePath());
                if (pdfSearchExcelOutputField.getText().trim().isEmpty()) {
                    updateSuggestion.run();
                }
            }
        });

        pdfSearchExcelPdfField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (pdfSearchExcelOutputField.getText().trim().isEmpty()) {
                updateSuggestion.run();
            }
        });
        pdfSearchExcelKeyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (pdfSearchExcelOutputField.getText().trim().isEmpty()) {
                updateSuggestion.run();
            }
        });

        pdfSearchExcelOutputBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            String suggestion = pdfSearchExcelOutputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.buildDefaultPdfSearchExcelName(pdfSearchExcelPdfField.getText(),
                        pdfSearchExcelKeyField.getText());
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
                pdfSearchExcelOutputField.setText(resolved.toAbsolutePath().toString());
                suggestionHolder = resolved.toAbsolutePath().toString();
            }
        });

        pdfSearchExcelButton.setOnAction(event -> {
            Task<PdfSearchExcelExtractor.ExtractionResult> task = new Task<>() {
                @Override
                protected PdfSearchExcelExtractor.ExtractionResult call() throws Exception {
                    return controller.extractPdfSearchToExcel(
                            pdfSearchExcelPdfField.getText(),
                            pdfSearchExcelKeyField.getText(),
                            pdfSearchExcelCaseSensitive.isSelected(),
                            pdfSearchExcelOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(pdfSearchExcelButton, pdfSearchExcelProgress, task);
            task.setOnSucceeded(e -> {
                PdfSearchExcelExtractor.ExtractionResult result = task.getValue();
                suggestionHolder = result.outputFile().toAbsolutePath().toString();
                String message = "Estrazione completata!\nRighe estratte: " + result.extractedRows()
                        + "\nFile creato: " + result.outputFile().toAbsolutePath();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'estrazione dal PDF.", true));
            new Thread(task, "pdf-search-excel-task").start();
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
