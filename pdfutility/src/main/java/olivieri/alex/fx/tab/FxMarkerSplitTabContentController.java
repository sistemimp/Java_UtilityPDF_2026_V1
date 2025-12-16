package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfMarkerSplitter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FxMarkerSplitTabContentController {
    @FXML
    private TextField markerPdfField;
    @FXML
    private Button markerPdfBrowse;
    @FXML
    private TextField markerTextField;
    @FXML
    private CheckBox markerCaseSensitive;
    @FXML
    private TextField markerBaseField;
    @FXML
    private Button markerBaseBrowse;
    @FXML
    private TextField markerFolderField;
    @FXML
    private CheckBox markerAppendCheck;
    @FXML
    private Button markerButton;
    @FXML
    private ProgressIndicator markerProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        markerFolderField.setText(controller.buildDefaultMarkerFolderName(markerTextField.getText()));

        markerPdfBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                markerPdfField.setText(selected.getAbsolutePath());
            }
        });

        markerBaseBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella base");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                markerBaseField.setText(selected.getAbsolutePath());
            }
        });

        markerTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (markerFolderField.getText().trim().isEmpty()) {
                markerFolderField.setText(controller.buildDefaultMarkerFolderName(newVal));
            }
        });

        markerButton.setOnAction(event -> {
            Task<PdfMarkerSplitter.Result> task = new Task<>() {
                @Override
                protected PdfMarkerSplitter.Result call() throws Exception {
                    return controller.splitByMarker(markerPdfField.getText(), markerTextField.getText(),
                            markerCaseSensitive.isSelected(), markerBaseField.getText(), markerFolderField.getText(),
                            markerAppendCheck.isSelected());
                }
            };
            FxTabControllerSupport.bindUiState(markerButton, markerProgress, task);
            task.setOnSucceeded(e -> {
                PdfMarkerSplitter.Result result = task.getValue();
                String message = "Split completato!\nDocumenti generati: " + result.getDocumentCount()
                        + "\nCartella risultati: " + result.getOutputDirectory();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante lo split."), owner));
            new Thread(task, "marker-split-task").start();
        });
    }
}
