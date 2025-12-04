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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxConditionalBlankTab {
    private FxConditionalBlankTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Pagine Bianche dopo testo");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseButton = new Button("Seleziona PDF");
        TextField phraseField = new TextField();
        CheckBox caseSensitiveCheck = new CheckBox("Rispetta maiuscole/minuscole");
        TextField outputField = new TextField();
        Button processButton = new Button("Inserisci pagine dopo testo");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                inputField.setText(selected.getAbsolutePath());
                updateSuggestion(outputField, controller, selected.toPath(), phraseField.getText());
            }
        });

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (outputField.getText().trim().isEmpty()) {
                try {
                    Path path = Paths.get(newVal.trim());
                    updateSuggestion(outputField, controller, path, phraseField.getText());
                } catch (Exception ignored) {
                    // wait for valid path
                }
            }
        });

        phraseField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (outputField.getText().trim().isEmpty()) {
                try {
                    Path path = Paths.get(inputField.getText().trim());
                    updateSuggestion(outputField, controller, path, newVal);
                } catch (Exception ignored) {
                    // wait for valid path
                }
            }
        });

        processButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.insertBlankAfterPhrase(inputField.getText(), phraseField.getText(),
                            caseSensitiveCheck.isSelected(), outputField.getText());
                }
            };
            bindUiState(processButton, progressIndicator, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Operazione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> showFailure(task.getException(), owner, "Errore durante l'elaborazione."));
            new Thread(task, "conditional-blank-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseButton, 2, 0);
        form.add(new Label("Frase o parola:"), 0, 1);
        form.add(phraseField, 1, 1, 2, 1);
        form.add(caseSensitiveCheck, 0, 2, 3, 1);
        form.add(new Label("File di output:"), 0, 3);
        form.add(outputField, 1, 3, 2, 1);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(phraseField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);

        HBox actionRow = new HBox(10, processButton, progressIndicator);
        actionRow.setPadding(new Insets(0, 10, 10, 10));

        BorderPane container = new BorderPane();
        container.setCenter(form);
        container.setBottom(actionRow);
        tab.setContent(container);
        return tab;
    }

    private static void updateSuggestion(TextField outputField, PdfUtilityFxController controller, Path path,
            String phrase) {
        if (outputField.getText().trim().isEmpty()) {
            outputField.setText(controller.buildDefaultBlankAfterPhraseName(path, phrase));
        }
    }

    private static void bindUiState(Button actionButton, ProgressIndicator indicator, Task<?> task) {
        actionButton.disableProperty().bind(task.runningProperty());
        indicator.visibleProperty().bind(task.runningProperty());
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
