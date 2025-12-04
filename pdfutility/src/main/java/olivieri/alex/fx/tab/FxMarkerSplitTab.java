package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import olivieri.alex.util.PdfMarkerSplitter;

import java.io.File;
import java.nio.file.Path;

public final class FxMarkerSplitTab {
    private FxMarkerSplitTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Split per stringa");
        tab.setClosable(false);

        TextField pdfField = new TextField();
        Button browsePdf = new Button("Seleziona PDF");
        TextField markerField = new TextField();
        CheckBox caseSensitive = new CheckBox("Rispetta maiuscole/minuscole");
        TextField outputDirField = new TextField();
        Button browseDir = new Button("Cartella base");
        TextField folderNameField = new TextField(controller.buildDefaultMarkerFolderName(markerField.getText()));
        CheckBox appendCheck = new CheckBox("Se esiste, aggiungi pagine al file esistente");
        Button splitButton = new Button("Esegui split");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browsePdf.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                pdfField.setText(selected.getAbsolutePath());
            }
        });

        browseDir.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella base");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                outputDirField.setText(selected.getAbsolutePath());
            }
        });

        markerField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (folderNameField.getText().trim().isEmpty()) {
                folderNameField.setText(controller.buildDefaultMarkerFolderName(newVal));
            }
        });

        splitButton.setOnAction(event -> {
            Task<PdfMarkerSplitter.Result> task = new Task<>() {
                @Override
                protected PdfMarkerSplitter.Result call() throws Exception {
                    return controller.splitByMarker(pdfField.getText(), markerField.getText(), caseSensitive.isSelected(),
                            outputDirField.getText(), folderNameField.getText(), appendCheck.isSelected());
                }
            };
            bindUiState(splitButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                PdfMarkerSplitter.Result result = task.getValue();
                String message = "Split completato!\nDocumenti generati: " + result.getDocumentCount()
                        + "\nCartella risultati: " + result.getOutputDirectory();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante lo split."), owner));
            new Thread(task, "marker-split-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(pdfField, 1, 0);
        form.add(browsePdf, 2, 0);
        form.add(new Label("Stringa marker:"), 0, 1);
        form.add(markerField, 1, 1, 2, 1);
        form.add(caseSensitive, 0, 2, 3, 1);
        form.add(new Label("Cartella base:"), 0, 3);
        form.add(outputDirField, 1, 3);
        form.add(browseDir, 2, 3);
        form.add(new Label("Nome cartella risultati:"), 0, 4);
        form.add(folderNameField, 1, 4, 2, 1);
        form.add(appendCheck, 0, 5, 3, 1);
        GridPane.setHgrow(pdfField, Priority.ALWAYS);
        GridPane.setHgrow(markerField, Priority.ALWAYS);
        GridPane.setHgrow(outputDirField, Priority.ALWAYS);
        GridPane.setHgrow(folderNameField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, splitButton, progressIndicator);
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
