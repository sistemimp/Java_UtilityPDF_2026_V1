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
import olivieri.alex.util.PdfToWordConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxPdfToWordTabContentController {
    @FXML
    private TextField pdfToWordInputField;
    @FXML
    private Button pdfToWordBrowsePdf;
    @FXML
    private Button pdfToWordBrowseDirectory;
    @FXML
    private TextField pdfToWordOutputField;
    @FXML
    private Button pdfToWordBrowseOutput;
    @FXML
    private Button pdfToWordConvertButton;
    @FXML
    private ProgressIndicator pdfToWordProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private String suggestionHolder = "";

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        Runnable updateSuggestion = () -> {
            String current = pdfToWordOutputField.getText().trim();
            if (!current.isEmpty() && !current.equals(suggestionHolder)) {
                return;
            }
            Path path = safePath(pdfToWordInputField.getText());
            if (path == null) {
                return;
            }
            if (Files.isDirectory(path)) {
                String suggestion = controller.buildDefaultWordDirectoryName(path);
                if (!suggestion.isEmpty()) {
                    pdfToWordOutputField.setText(suggestion);
                    suggestionHolder = suggestion;
                }
            } else if (Files.isRegularFile(path)) {
                String suggestion = controller.buildDefaultWordName(path);
                if (!suggestion.isEmpty()) {
                    pdfToWordOutputField.setText(suggestion);
                    suggestionHolder = suggestion;
                }
            }
        };

        pdfToWordBrowsePdf.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                pdfToWordInputField.setText(selected.getAbsolutePath());
                updateSuggestion.run();
            }
        });

        pdfToWordBrowseDirectory.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                pdfToWordInputField.setText(selected.getAbsolutePath());
                updateSuggestion.run();
            }
        });

        pdfToWordBrowseOutput.setOnAction(event -> {
            Path path = safePath(pdfToWordInputField.getText());
            boolean directoryInput = path != null && Files.isDirectory(path);
            if (directoryInput) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Seleziona cartella di output");
                String suggestion = pdfToWordOutputField.getText().trim();
                if (suggestion.isEmpty()) {
                    suggestion = controller.buildDefaultWordDirectoryName(path);
                }
                if (!suggestion.isEmpty()) {
                    try {
                        Path suggestionPath = Paths.get(suggestion);
                        if (Files.isDirectory(suggestionPath)) {
                            chooser.setInitialDirectory(suggestionPath.toFile());
                        } else if (suggestionPath.getParent() != null
                                && Files.isDirectory(suggestionPath.getParent())) {
                            chooser.setInitialDirectory(suggestionPath.getParent().toFile());
                        }
                    } catch (Exception ignored) {
                        // ignore invalid suggestion
                    }
                }
                File selected = chooser.showDialog(owner);
                if (selected != null) {
                    pdfToWordOutputField.setText(selected.getAbsolutePath());
                }
            } else {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Salva file Word");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (*.docx)", "*.docx"));
                String suggestion = pdfToWordOutputField.getText().trim();
                if (suggestion.isEmpty()) {
                    suggestion = controller.buildDefaultWordName(path);
                }
                if (!suggestion.isEmpty()) {
                    try {
                        Path suggestionPath = Paths.get(suggestion);
                        chooser.setInitialFileName(suggestionPath.getFileName().toString());
                        Path parent = suggestionPath.getParent();
                        if (parent != null && Files.isDirectory(parent)) {
                            chooser.setInitialDirectory(parent.toFile());
                        }
                    } catch (Exception ignored) {
                        // ignore invalid suggestion
                    }
                }
                File selected = chooser.showSaveDialog(owner);
                if (selected != null) {
                    Path resolved = ensureExtension(selected.toPath(), ".docx");
                    pdfToWordOutputField.setText(resolved.toAbsolutePath().toString());
                }
            }
        });

        pdfToWordConvertButton.setOnAction(event -> {
            Task<Object> task = new Task<>() {
                @Override
                protected Object call() throws Exception {
                    Path path = safePath(pdfToWordInputField.getText());
                    if (path != null && Files.isDirectory(path)) {
                        return controller.convertDirectoryToWord(pdfToWordInputField.getText(), pdfToWordOutputField.getText());
                    }
                    return controller.convertPdfToWord(pdfToWordInputField.getText(), pdfToWordOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(pdfToWordConvertButton, pdfToWordProgress, task);
            task.setOnSucceeded(e -> {
                Object result = task.getValue();
                if (result instanceof PdfToWordConverter.BatchResult batch) {
                    String message = "Conversione completata!\nDocumenti creati: " + batch.getConvertedCount()
                            + "\nCartella output: " + batch.getOutputDirectory().toAbsolutePath();
                    suggestionHolder = batch.getOutputDirectory().toAbsolutePath().toString();
                    FxDialogUtils.showInformation("Successo", message, owner);
                    return;
                }
                if (result instanceof Path pathResult) {
                    suggestionHolder = pathResult.toAbsolutePath().toString();
                    FxDialogUtils.showInformation("Successo",
                            "Conversione completata!\nFile creato: " + pathResult.toAbsolutePath(), owner);
                }
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante la conversione."), owner));
            new Thread(task, "pdf-to-word-task").start();
        });
    }

    private static Path safePath(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Paths.get(text.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path ensureExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        if (!filename.toLowerCase().endsWith(extension)) {
            path = path.resolveSibling(filename + extension);
        }
        return path;
    }
}
