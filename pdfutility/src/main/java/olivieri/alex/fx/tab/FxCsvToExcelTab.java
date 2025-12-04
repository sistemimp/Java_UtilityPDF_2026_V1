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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxCsvToExcelTab {
    private FxCsvToExcelTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("CSV in Excel");
        tab.setClosable(false);

        TextField csvField = new TextField();
        Button browseCsv = new Button("CSV");
        TextField excelField = new TextField();
        Button browseExcel = new Button("Excel");
        Button convertButton = new Button("Converti");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseCsv.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                csvField.setText(selected.getAbsolutePath());
            }
        });

        csvField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!excelField.getText().trim().isEmpty()) {
                return;
            }
            String suggestion = controller.buildDefaultExcelName(newVal);
            if (!suggestion.isEmpty()) {
                excelField.setText(suggestion);
            }
        });

        browseExcel.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
            String suggestion = excelField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.buildDefaultExcelName(csvField.getText());
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
                excelField.setText(resolved.toAbsolutePath().toString());
            }
        });

        convertButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.convertCsvToExcel(csvField.getText(), excelField.getText());
                }
            };
            bindUiState(convertButton, progressIndicator, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Conversione completata!\nFile creato: " + task.getValue().toString(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante la conversione."), owner));
            new Thread(task, "csv-to-excel-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("File CSV:"), 0, 0);
        form.add(csvField, 1, 0);
        form.add(browseCsv, 2, 0);
        form.add(new Label("File Excel:"), 0, 1);
        form.add(excelField, 1, 1);
        form.add(browseExcel, 2, 1);
        GridPane.setHgrow(csvField, Priority.ALWAYS);
        GridPane.setHgrow(excelField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, convertButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
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
