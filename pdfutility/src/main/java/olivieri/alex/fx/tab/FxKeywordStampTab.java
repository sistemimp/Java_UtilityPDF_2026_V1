package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfKeywordStamper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxKeywordStampTab {
    private FxKeywordStampTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Timbro per parola");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseInput = new Button("PDF");
        TextField keywordField = new TextField();
        TextField stampField = new TextField();
        TextField outputField = new TextField();
        Button browseOutput = new Button("Output");
        CheckBox caseSensitive = new CheckBox("Rispetta maiuscole/minuscole");
        Spinner<Double> xSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        Spinner<Double> ySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        Button stampButton = new Button("Applica timbro");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);

        browseInput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                inputField.setText(selected.getAbsolutePath());
            }
        });

        Runnable suggestOutput = () -> {
            String current = outputField.getText().trim();
            if (!current.isEmpty()) {
                return;
            }
            String candidateInput = inputField.getText().trim();
            String keyword = keywordField.getText().trim();
            if (candidateInput.isEmpty() || keyword.isEmpty()) {
                return;
            }
            try {
                Path path = Paths.get(candidateInput);
                if (!Files.isRegularFile(path)) {
                    return;
                }
                String suggestion = controller.buildDefaultKeywordStampName(path, keyword);
                if (!suggestion.isEmpty()) {
                    outputField.setText(suggestion);
                }
            } catch (Exception ignored) {
                // ignore invalid path
            }
        };

        inputField.textProperty().addListener((obs, oldVal, newVal) -> suggestOutput.run());
        keywordField.textProperty().addListener((obs, oldVal, newVal) -> suggestOutput.run());

        browseOutput.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Salva PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            String suggestion = outputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.buildDefaultKeywordStampName(getPathFrom(inputField.getText()), keywordField.getText());
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
                Path resolved = ensureExtension(selected.toPath(), ".pdf");
                outputField.setText(resolved.toAbsolutePath().toString());
            }
        });

        stampButton.setOnAction(event -> {
            Task<PdfKeywordStamper.Result> task = new Task<>() {
                @Override
                protected PdfKeywordStamper.Result call() throws Exception {
                    return controller.stampKeyword(inputField.getText(), outputField.getText(), keywordField.getText(),
                            stampField.getText(), caseSensitive.isSelected(), xSpinner.getValue().floatValue(),
                            ySpinner.getValue().floatValue());
                }
            };
            bindUiState(stampButton, progressIndicator, task);
            task.setOnSucceeded(e -> {
                PdfKeywordStamper.Result result = task.getValue();
                String message = "Timbro completato!\nPagine timbrate: " + result.getStampedPages() + " su "
                        + result.getTotalPages() + "\nFile output: " + result.getOutputFile();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante l'applicazione del timbro."), owner));
            new Thread(task, "keyword-stamp-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseInput, 2, 0);
        form.add(new Label("Parola chiave:"), 0, 1);
        form.add(keywordField, 1, 1, 2, 1);
        form.add(new Label("Testo timbro:"), 0, 2);
        form.add(stampField, 1, 2, 2, 1);
        form.add(new Label("File di output:"), 0, 3);
        form.add(outputField, 1, 3);
        form.add(browseOutput, 2, 3);
        form.add(caseSensitive, 0, 4, 3, 1);
        form.add(new Label("Coordinata X:"), 0, 5);
        form.add(xSpinner, 1, 5);
        form.add(new Label("Coordinata Y:"), 0, 6);
        form.add(ySpinner, 1, 6);
        GridPane.setHgrow(inputField, Priority.ALWAYS);
        GridPane.setHgrow(keywordField, Priority.ALWAYS);
        GridPane.setHgrow(stampField, Priority.ALWAYS);
        GridPane.setHgrow(outputField, Priority.ALWAYS);
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

    private static Path getPathFrom(String value) {
        try {
            return Paths.get(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path ensureExtension(Path path, String extension) {
        String filename = path.getFileName().toString();
        if (!filename.toLowerCase().endsWith(extension)) {
            path = path.resolveSibling(filename + extension);
        }
        return path;
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
