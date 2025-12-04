package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

public final class FxPdfToWordTab {
    private FxPdfToWordTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("PDF in Word");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browsePdf = new Button("PDF");
        Button browseDirectory = new Button("Cartella");
        TextField outputField = new TextField();
        Button browseOutput = new Button("Output");
        Button convertButton = new Button("Converti");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        String[] suggestionHolder = { "" };
        Runnable updateSuggestion = () -> {
            String current = outputField.getText().trim();
            if (!current.isEmpty() && !current.equals(suggestionHolder[0])) {
                return;
            }
            Path path = safePath(inputField.getText());
            if (path == null) {
                return;
            }
            if (Files.isDirectory(path)) {
                String suggestion = controller.buildDefaultWordDirectoryName(path);
                if (!suggestion.isEmpty()) {
                    outputField.setText(suggestion);
                    suggestionHolder[0] = suggestion;
                }
            } else if (Files.isRegularFile(path)) {
                String suggestion = controller.buildDefaultWordName(path);
                if (!suggestion.isEmpty()) {
                    outputField.setText(suggestion);
                    suggestionHolder[0] = suggestion;
                }
            }
        };

        browsePdf.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                inputField.setText(selected.getAbsolutePath());
                updateSuggestion.run();
            }
        });

        browseDirectory.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                inputField.setText(selected.getAbsolutePath());
                updateSuggestion.run();
            }
        });

        browseOutput.setOnAction(event -> {
            Path path = safePath(inputField.getText());
            boolean directoryInput = path != null && Files.isDirectory(path);
            if (directoryInput) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Seleziona cartella di output");
                String suggestion = outputField.getText().trim();
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
                    outputField.setText(selected.getAbsolutePath());
                }
            } else {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Salva file Word");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (*.docx)", "*.docx"));
                String suggestion = outputField.getText().trim();
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
                    outputField.setText(resolved.toAbsolutePath().toString());
                }
            }
        });

        convertButton.setOnAction(event -> {
            Task<Object> task = new Task<>() {
                @Override
                protected Object call() throws Exception {
                    Path path = safePath(inputField.getText());
                    if (path != null && Files.isDirectory(path)) {
                        return controller.convertDirectoryToWord(inputField.getText(), outputField.getText());
                    }
                    return controller.convertPdfToWord(inputField.getText(), outputField.getText());
                }
            };
            bindUiState(convertButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                Object result = task.getValue();
                if (result instanceof PdfToWordConverter.BatchResult batch) {
                    String message = "Conversione completata!\nDocumenti creati: " + batch.getConvertedCount()
                            + "\nCartella output: " + batch.getOutputDirectory().toAbsolutePath();
                    suggestionHolder[0] = batch.getOutputDirectory().toAbsolutePath().toString();
                    FxDialogUtils.showInformation("Successo", message, owner);
                    return;
                }
                if (result instanceof Path pathResult) {
                    suggestionHolder[0] = pathResult.toAbsolutePath().toString();
                    FxDialogUtils.showInformation("Successo",
                            "Conversione completata!\nFile creato: " + pathResult.toAbsolutePath(), owner);
                }
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante la conversione."), owner));
            new Thread(task, "pdf-to-word-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF/cartella:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(new HBox(10, browsePdf, browseDirectory), 2, 0);
        form.add(new Label("Output (file o cartella):"), 0, 1);
        form.add(outputField, 1, 1);
        form.add(browseOutput, 2, 1);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, convertButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
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

    private static void bindUiState(Button actionButton, ProgressIndicator indicator, Task<?> task) {
        actionButton.disableProperty().bind(task.runningProperty());
        indicator.visibleProperty().bind(task.runningProperty());
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
