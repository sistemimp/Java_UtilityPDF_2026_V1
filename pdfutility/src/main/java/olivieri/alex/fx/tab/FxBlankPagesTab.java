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
import java.nio.file.Path;

public final class FxBlankPagesTab {
    private FxBlankPagesTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Pagine Bianche");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseButton = new Button("Seleziona PDF");
        TextField outputField = new TextField();
        Button processButton = new Button("Aggiungi pagine bianche");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File chosen = chooser.showOpenDialog(owner);
            if (chosen != null) {
                inputField.setText(chosen.getAbsolutePath());
                Path pdfPath = chosen.toPath();
                if (outputField.getText().trim().isEmpty()) {
                    outputField.setText(controller.buildDefaultBlankPagesName(pdfPath));
                }
            }
        });

        processButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.insertBlankPages(inputField.getText(), outputField.getText());
                }
            };
            bindUiState(processButton, progressIndicator, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Operazione completata!\nFile creato: " + task.getValue().toString(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'elaborazione."), owner));
            new Thread(task, "blank-pages-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseButton, 2, 0);
        form.add(new Label("File di output:"), 0, 1);
        form.add(outputField, 1, 1, 2, 1);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, processButton, progressIndicator);
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
