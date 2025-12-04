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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FxOptimizeTab {
    private FxOptimizeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Ottimizzazione PDF");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseButton = new Button("Seleziona PDF/Cartella");
        TextField outputField = new TextField();
        Button optimizeButton = new Button("Ottimizza");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseButton.setOnAction(event -> {
            if (chooseDirectory(inputField, outputField, owner)) {
                return;
            }
            chooseFile(inputField, outputField, controller, owner);
        });

        optimizeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.optimize(inputField.getText(), outputField.getText());
                }
            };
            bindUiState(optimizeButton, progressIndicator, task);
            task.setOnSucceeded(e -> showSuccess(task.getValue(), owner));
            task.setOnFailed(e -> showFailure(task.getException(), owner, "Errore durante l'ottimizzazione."));
            new Thread(task, "optimize-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF o cartella:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseButton, 2, 0);
        form.add(new Label("File ottimizzato:"), 0, 1);
        form.add(outputField, 1, 1, 2, 1);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, optimizeButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
    }

    private static boolean chooseDirectory(TextField inputField, TextField outputField, Window owner) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella");
        File selected = chooser.showDialog(owner);
        if (selected != null) {
            inputField.setText(selected.getAbsolutePath());
            outputField.setText("");
            return true;
        }
        return false;
    }

    private static void chooseFile(TextField inputField, TextField outputField, PdfUtilityFxController controller,
            Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleziona PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File selected = chooser.showOpenDialog(owner);
        if (selected != null) {
            inputField.setText(selected.getAbsolutePath());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(controller.buildDefaultOptimizedName(selected.toPath()));
            }
        }
    }

    private static void bindUiState(Button actionButton, ProgressIndicator indicator, Task<?> task) {
        actionButton.disableProperty().bind(task.runningProperty());
        indicator.visibleProperty().bind(task.runningProperty());
    }

    private static void showSuccess(Path result, Window owner) {
        String summary = Files.isDirectory(result) ? "Cartella creata: " : "File creato: ";
        FxDialogUtils.showInformation("Successo", "Ottimizzazione completata!\n" + summary + result, owner);
    }

    private static void showFailure(Throwable throwable, Window owner, String fallbackMessage) {
        Throwable root = throwable;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        String message = root != null ? root.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage;
        }
        if (root instanceof IllegalArgumentException) {
            FxDialogUtils.showWarning("Attenzione", message, owner);
        } else {
            FxDialogUtils.showError("Errore", message, owner);
        }
    }
}
