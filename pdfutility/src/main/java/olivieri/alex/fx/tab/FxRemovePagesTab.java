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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfStringPageRemover;

import java.io.File;
import java.nio.file.Path;

public final class FxRemovePagesTab {
    private FxRemovePagesTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Rimuovi pagine");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseInput = new Button("Seleziona PDF");
        TextField outputField = new TextField();
        Button browseOutput = new Button("File di output");
        TextField searchField = new TextField();
        CheckBox caseSensitive = new CheckBox("Rispetta maiuscole/minuscole");
        Button removeButton = new Button("Rimuovi pagine");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseInput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                inputField.setText(selected.getAbsolutePath());
                updateSuggestion(controller, inputField, searchField, outputField);
            }
        });

        browseOutput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Salva PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            String suggestion = outputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.suggestRemovalOutputName(inputField.getText(), searchField.getText());
            }
            if (!suggestion.isEmpty()) {
                chooser.setInitialFileName(Path.of(suggestion).getFileName().toString());
            }
            File selected = chooser.showSaveDialog(owner);
            if (selected != null) {
                outputField.setText(selected.getAbsolutePath());
            }
        });

        inputField.textProperty().addListener((obs, oldVal, newVal) -> updateSuggestion(controller, inputField,
                searchField, outputField));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateSuggestion(controller, inputField,
                searchField, outputField));

        removeButton.setOnAction(event -> {
            Task<PdfStringPageRemover.Result> task = new Task<>() {
                @Override
                protected PdfStringPageRemover.Result call() throws Exception {
                    return controller.removePagesContaining(inputField.getText(), outputField.getText(),
                            searchField.getText(), caseSensitive.isSelected());
                }
            };
            bindUiState(removeButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                PdfStringPageRemover.Result result = task.getValue();
                String message = new StringBuilder().append("Rimozione completata!\nPagine rimosse: ")
                        .append(result.getRemovedPages()).append("\nPagine restanti: ")
                        .append(result.getRetainedPages()).append("\nFile creato: ")
                        .append(result.getOutputFile()).toString();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'elaborazione."), owner));
            new Thread(task, "remove-pages-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseInput, 2, 0);
        form.add(new Label("PDF output:"), 0, 1);
        form.add(outputField, 1, 1);
        form.add(browseOutput, 2, 1);
        form.add(new Label("Stringa da cercare:"), 0, 2);
        form.add(searchField, 1, 2, 2, 1);
        form.add(caseSensitive, 1, 3, 2, 1);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);
        GridPane.setHgrow(searchField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, removeButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
    }

    private static void updateSuggestion(PdfUtilityFxController controller, TextField inputField, TextField searchField,
            TextField outputField) {
        if (controller == null || inputField == null || searchField == null || outputField == null) {
            return;
        }
        if (outputField.getText().trim().isEmpty()) {
            String suggestion = controller.suggestRemovalOutputName(inputField.getText(), searchField.getText());
            if (!suggestion.isEmpty()) {
                outputField.setText(suggestion);
            }
        }
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
