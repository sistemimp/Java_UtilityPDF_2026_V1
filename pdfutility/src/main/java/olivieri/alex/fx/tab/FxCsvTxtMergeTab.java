package olivieri.alex.fx.tab;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FxCsvTxtMergeTab {
    private FxCsvTxtMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Unisci CSV/TXT");
        tab.setClosable(false);

        ListView<String> fileListView = new ListView<>(FXCollections.observableArrayList());
        fileListView.setPrefHeight(200);
        Button addButton = new Button("Aggiungi file");
        Button removeButton = new Button("Rimuovi selezionati");
        TextField outputField = new TextField();
        Button browseOutput = new Button("File di output");
        Button mergeButton = new Button("Unisci");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        String[] lastSuggestion = { "" };
        Runnable updateSuggestion = () -> {
            if (fileListView.getItems().isEmpty()) {
                lastSuggestion[0] = "";
                return;
            }
            String current = outputField.getText().trim();
            try {
                Path candidate = Paths.get(fileListView.getItems().get(0));
                String suggestion = controller.buildDefaultMergedTextName(candidate);
                if (suggestion.isEmpty()) {
                    return;
                }
                if (current.isEmpty() || current.equals(lastSuggestion[0])) {
                    outputField.setText(suggestion);
                    lastSuggestion[0] = suggestion;
                }
            } catch (Exception ignored) {
                // ignore invalid first entry
            }
        };

        addButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV/TXT");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV o TXT", "*.csv", "*.txt"));
            List<File> selected = chooser.showOpenMultipleDialog(owner);
            if (selected != null) {
                for (File file : selected) {
                    if (file != null) {
                        fileListView.getItems().add(file.getAbsolutePath());
                    }
                }
                updateSuggestion.run();
            }
        });

        removeButton.setOnAction(event -> {
            List<String> selected = new ArrayList<>(fileListView.getSelectionModel().getSelectedItems());
            fileListView.getItems().removeAll(selected);
            updateSuggestion.run();
        });

        browseOutput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file di output");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV o TXT", "*.csv", "*.txt"));
            String suggestion = outputField.getText().trim();
            if (suggestion.isEmpty() && !fileListView.getItems().isEmpty()) {
                try {
                    suggestion = controller.buildDefaultMergedTextName(Paths.get(fileListView.getItems().get(0)));
                } catch (Exception ignored) {
                    // ignore
                }
            }
            if (!suggestion.isEmpty()) {
                try {
                    Path path = Paths.get(suggestion);
                    chooser.setInitialFileName(path.getFileName().toString());
                    Path parent = path.getParent();
                    if (parent != null) {
                        chooser.setInitialDirectory(parent.toFile());
                    }
                } catch (Exception ignored) {
                    // ignore invalid suggestion
                }
            }
            File selected = chooser.showSaveDialog(owner);
            if (selected != null) {
                String filename = selected.getName();
                String lower = filename.toLowerCase();
                Path resolved;
                if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
                    resolved = selected.toPath();
                } else {
                    String extension = ".csv";
                    resolved = selected.toPath().resolveSibling(filename + extension);
                }
                outputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        mergeButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    List<String> inputs = new ArrayList<>(fileListView.getItems());
                    return controller.mergeCsvTxt(inputs, outputField.getText());
                }
            };
            bindUiState(mergeButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                Path result = task.getValue();
                lastSuggestion[0] = result.toAbsolutePath().toString();
                FxDialogUtils.showInformation("Successo",
                        "Unione completata!\nFile creato: " + result.toAbsolutePath(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'unione."), owner));
            new Thread(task, "csv-txt-merge-task").start();
        });

        VBox listSection = new VBox(5, new Label("File da unire:"), fileListView);
        listSection.setPadding(new Insets(10));

        HBox listActions = new HBox(10, addButton, removeButton);
        listActions.setPadding(new Insets(0, 10, 0, 10));

        GridPane outputPanel = new GridPane();
        outputPanel.setHgap(10);
        outputPanel.setVgap(10);
        outputPanel.setPadding(new Insets(0, 10, 10, 10));
        outputPanel.add(new Label("File di output:"), 0, 0);
        outputPanel.add(outputField, 1, 0);
        outputPanel.add(browseOutput, 2, 0);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, mergeButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setTop(listActions);
        container.setCenter(listSection);
        VBox bottomArea = new VBox(outputPanel, actionRow);
        container.setBottom(bottomArea);
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
