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
import olivieri.alex.util.PdfCsvRenamer;

import java.io.File;

public final class FxCsvRenameTab {
    private FxCsvRenameTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Rinomina da CSV");
        tab.setClosable(false);

        TextField directoryField = new TextField();
        Button browseDirectory = new Button("Cartella PDF");
        TextField csvField = new TextField();
        Button browseCsv = new Button("CSV");
        Button renameButton = new Button("Rinomina");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseDirectory.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                directoryField.setText(selected.getAbsolutePath());
            }
        });

        browseCsv.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                csvField.setText(selected.getAbsolutePath());
            }
        });

        renameButton.setOnAction(event -> {
            Task<PdfCsvRenamer.Result> task = new Task<>() {
                @Override
                protected PdfCsvRenamer.Result call() throws Exception {
                    return controller.renameFromCsv(directoryField.getText(), csvField.getText());
                }
            };
            bindUiState(renameButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                PdfCsvRenamer.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Rinomina completata!\nFile rinominati: ")
                        .append(result.getRenamedCount());
                if (result.hasWarnings()) {
                    message.append("\nAvvisi:");
                    for (String warning : result.getWarnings()) {
                        message.append("\n- ").append(warning);
                    }
                }
                FxDialogUtils.showInformation("Successo", message.toString(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante la rinomina."), owner));
            new Thread(task, "csv-rename-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Cartella PDF:"), 0, 0);
        form.add(directoryField, 1, 0);
        form.add(browseDirectory, 2, 0);
        form.add(new Label("File CSV:"), 0, 1);
        form.add(csvField, 1, 1);
        form.add(browseCsv, 2, 1);
        GridPane.setHgrow(directoryField, Priority.ALWAYS);
        GridPane.setHgrow(csvField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, renameButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
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
