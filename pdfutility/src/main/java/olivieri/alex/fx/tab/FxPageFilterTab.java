package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfPageFilter;

import java.io.File;
import java.nio.file.Path;

public final class FxPageFilterTab {
    private FxPageFilterTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        Tab tab = new Tab("Filtro pagine");
        tab.setClosable(false);

        TextField inputField = new TextField();
        Button browseButton = new Button("Seleziona PDF");
        RadioButton removeOdd = new RadioButton("Rimuovi pagine dispari");
        RadioButton removeEven = new RadioButton("Rimuovi pagine pari");
        removeOdd.setSelected(true);
        ToggleGroup group = new ToggleGroup();
        removeOdd.setToggleGroup(group);
        removeEven.setToggleGroup(group);
        TextField outputField = new TextField();
        Button processButton = new Button("Filtra pagine");
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
                if (outputField.getText().trim().isEmpty()) {
                    updateSuggestion(controller, inputField, outputField, getMode(removeOdd));
                }
            }
        });

        inputField.textProperty().addListener((obs, oldVal, newVal) -> updateSuggestion(controller, inputField, outputField,
                getMode(removeOdd)));
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateSuggestion(controller, inputField,
                outputField, getMode(removeOdd)));

        processButton.setOnAction(event -> {
            PdfPageFilter.Mode mode = getMode(removeOdd);
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.filterPages(inputField.getText(), mode, outputField.getText());
                }
            };
            bindUiState(processButton, progressIndicator, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Filtraggio completato!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    getRootCauseMessage(task.getException(), "Errore durante il filtraggio."), owner));
            new Thread(task, "page-filter-task").start();
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("PDF sorgente:"), 0, 0);
        form.add(inputField, 1, 0);
        form.add(browseButton, 2, 0);
        form.add(removeOdd, 0, 1, 3, 1);
        form.add(removeEven, 0, 2, 3, 1);
        form.add(new Label("File di output:"), 0, 3);
        form.add(outputField, 1, 3, 2, 1);
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

    private static void updateSuggestion(PdfUtilityFxController controller, TextField inputField, TextField outputField,
            PdfPageFilter.Mode mode) {
        if (controller == null || inputField == null || outputField == null) {
            return;
        }
        if (outputField.getText().trim().isEmpty()) {
            String suggestion = controller.suggestFilteredOutputName(inputField.getText(), mode);
            if (!suggestion.isEmpty()) {
                outputField.setText(suggestion);
            }
        }
    }

    private static PdfPageFilter.Mode getMode(RadioButton removeOdd) {
        return removeOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN;
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
