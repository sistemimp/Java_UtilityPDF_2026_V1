package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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
import olivieri.alex.util.PdfFolderStamper;

import java.io.File;

public final class FxFolderStampTab {
    private FxFolderStampTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Timbro cartella");
        tab.setClosable(false);

        TextField directoryField = new TextField();
        Button browseDirectory = new Button("Cartella PDF");
        TextField stampField = new TextField();
        Spinner<Double> xSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        Spinner<Double> ySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        Button stampButton = new Button("Applica timbro");
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

        stampButton.setOnAction(event -> {
            Task<PdfFolderStamper.Result> task = new Task<>() {
                @Override
                protected PdfFolderStamper.Result call() throws Exception {
                    return controller.stampFolder(directoryField.getText(), stampField.getText(),
                            xSpinner.getValue().floatValue(), ySpinner.getValue().floatValue());
                }
            };
            bindUiState(stampButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                PdfFolderStamper.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Timbro completato!\nFile elaborati: ")
                        .append(result.getStampedCount()).append("\nCartella output: ")
                        .append(result.getOutputDirectory());
                if (result.hasWarnings()) {
                    message.append("\nAvvisi:");
                    for (String warning : result.getWarnings()) {
                        message.append("\n- ").append(warning);
                    }
                }
                FxDialogUtils.showInformation("Successo", message.toString(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'applicazione del timbro."), owner));
            new Thread(task, "folder-stamp-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Cartella PDF:"), 0, 0);
        form.add(directoryField, 1, 0);
        form.add(browseDirectory, 2, 0);
        form.add(new Label("Testo timbro:"), 0, 1);
        form.add(stampField, 1, 1, 2, 1);
        form.add(new Label("Coordinate X:"), 0, 2);
        form.add(xSpinner, 1, 2);
        form.add(new Label("Coordinate Y:"), 0, 3);
        form.add(ySpinner, 1, 3);
        GridPane.setHgrow(directoryField, Priority.ALWAYS);
        GridPane.setHgrow(stampField, Priority.ALWAYS);
        GridPane.setHgrow(xSpinner, Priority.ALWAYS);
        GridPane.setHgrow(ySpinner, Priority.ALWAYS);

        HBox actionRow = new HBox(10, stampButton, progressIndicator);
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
