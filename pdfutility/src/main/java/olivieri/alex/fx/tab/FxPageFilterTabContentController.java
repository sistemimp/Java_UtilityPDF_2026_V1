package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfPageFilter;

import java.io.File;
import java.nio.file.Path;

public final class FxPageFilterTabContentController {
    @FXML
    private TextField filterInputField;
    @FXML
    private Button filterBrowseButton;
    @FXML
    private RadioButton filterRemoveOdd;
    @FXML
    private RadioButton filterRemoveEven;
    @FXML
    private TextField filterOutputField;
    @FXML
    private Button filterProcessButton;
    @FXML
    private ProgressIndicator filterProgress;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        filterRemoveOdd.setToggleGroup(toggleGroup);
        filterRemoveEven.setToggleGroup(toggleGroup);
        filterRemoveOdd.setSelected(true);

        filterBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                filterInputField.setText(selected.getAbsolutePath());
                updateSuggestion(controller, filterInputField, filterOutputField, getMode());
            }
        });

        filterInputField.textProperty().addListener((obs, oldVal, newVal) ->
                updateSuggestion(controller, filterInputField, filterOutputField, getMode()));
        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) ->
                updateSuggestion(controller, filterInputField, filterOutputField, getMode()));

        filterProcessButton.setOnAction(event -> {
            PdfPageFilter.Mode mode = getMode();
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.filterPages(filterInputField.getText(), mode, filterOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(filterProcessButton, filterProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Filtraggio completato!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante il filtraggio."), owner));
            new Thread(task, "page-filter-task").start();
        });
    }

    private PdfPageFilter.Mode getMode() {
        return filterRemoveOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN;
    }

    private static void updateSuggestion(PdfUtilityFxController controller, TextField inputField,
            TextField outputField, PdfPageFilter.Mode mode) {
        if (controller == null || inputField == null || outputField == null) {
            return;
        }
        if (outputField.getText().trim().isEmpty()) {
            String candidate = inputField.getText() == null ? "" : inputField.getText().trim();
            if (candidate.isEmpty()) {
                return;
            }
            String suggestion = controller.suggestFilteredOutputName(candidate, mode);
            if (!suggestion.isEmpty()) {
                outputField.setText(suggestion);
            }
        }
    }
}
