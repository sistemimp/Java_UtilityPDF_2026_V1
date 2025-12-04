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
import olivieri.alex.util.PdfPairMerger;

import java.io.File;
import java.nio.file.Path;

public final class FxPairMergeTab {
    private FxPairMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Unisci per nome");
        tab.setClosable(false);

        TextField firstDirField = new TextField();
        Button firstBrowse = new Button("Cartella 1");
        TextField secondDirField = new TextField();
        Button secondBrowse = new Button("Cartella 2");
        Button mergeButton = new Button("Unisci");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        firstBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona prima cartella");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                firstDirField.setText(selected.getAbsolutePath());
            }
        });
        secondBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona seconda cartella");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                secondDirField.setText(selected.getAbsolutePath());
            }
        });

        mergeButton.setOnAction(event -> {
            Task<PdfPairMerger.Result> task = new Task<>() {
                @Override
                protected PdfPairMerger.Result call() throws Exception {
                    return controller.mergeMatchingPairs(firstDirField.getText(), secondDirField.getText());
                }
            };
            bindUiState(mergeButton, progressIndicator, task);
            task.setOnSucceeded(e -> showSuccess(task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'unione per nome."), owner));
            new Thread(task, "pair-merge-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Cartella 1:"), 0, 0);
        form.add(firstDirField, 1, 0);
        form.add(firstBrowse, 2, 0);
        form.add(new Label("Cartella 2:"), 0, 1);
        form.add(secondDirField, 1, 1);
        form.add(secondBrowse, 2, 1);
        GridPane.setHgrow(firstDirField, Priority.ALWAYS);
        GridPane.setHgrow(secondDirField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, mergeButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
    }

    private static void showSuccess(PdfPairMerger.Result result, Window owner) {
        StringBuilder message = new StringBuilder().append("Unione completata!\nFile generati: ")
                .append(result.getMergedCount()).append("\nCartella risultante: ").append(result.getOutputDirectory());
        if (result.hasMissingReport()) {
            message.append("\nFile mancanti: ").append(result.getMissingReport());
        }
        FxDialogUtils.showInformation("Successo", message.toString(), owner);
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
