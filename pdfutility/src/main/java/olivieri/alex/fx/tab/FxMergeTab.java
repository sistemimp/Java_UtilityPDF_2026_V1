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
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;

public final class FxMergeTab {
    private FxMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Unione PDF");
        tab.setClosable(false);

        TextField directoryField = new TextField();
        Button browseButton = new Button("Sfoglia cartella");
        TextField outputField = new TextField("#_merge.pdf");
        Button mergeButton = new Button("Unisci");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                directoryField.setText(selected.getAbsolutePath());
            }
        });

        mergeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.mergeDirectory(directoryField.getText(), outputField.getText());
                }
            };
            bindUiState(mergeButton, progressIndicator, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Unione completata!\nFile creato: " + task.getValue().toString(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'unione."), owner));
            new Thread(task, "merge-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Cartella PDF:"), 0, 0);
        form.add(directoryField, 1, 0);
        form.add(browseButton, 2, 0);
        form.add(new Label("File di output:"), 0, 1);
        form.add(outputField, 1, 1, 2, 1);
        GridPane.setHgrow(directoryField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, mergeButton, progressIndicator);
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
