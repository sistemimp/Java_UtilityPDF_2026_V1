package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfShipmentSplitter;

import java.io.File;

public final class FxShipmentSplitTabContentController {
    @FXML
    private TextField shipmentPdfField;
    @FXML
    private Button shipmentPdfBrowse;
    @FXML
    private TextField shipmentMarkerField;
    @FXML
    private CheckBox shipmentCaseSensitive;
    @FXML
    private Spinner<Integer> shipmentCountSpinner;
    @FXML
    private TextField shipmentBaseField;
    @FXML
    private Button shipmentBaseBrowse;
    @FXML
    private TextField shipmentFolderField;
    @FXML
    private TextField shipmentPrefixField;
    @FXML
    private Button shipmentButton;
    @FXML
    private ProgressIndicator shipmentProgress;

    private PdfUtilityFxController controller;
    private Window owner;
    private boolean folderEdited;
    private boolean prefixEdited;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;
        shipmentCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 50));
        updateFolderSuggestion();
        updatePrefixSuggestion();

        shipmentFolderField.textProperty().addListener((obs, oldValue, newValue) -> {
            folderEdited = newValue != null && !newValue.trim().isEmpty();
        });
        shipmentPrefixField.textProperty().addListener((obs, oldValue, newValue) -> {
            prefixEdited = newValue != null && !newValue.trim().isEmpty();
        });

        shipmentPdfBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                shipmentPdfField.setText(selected.getAbsolutePath());
                if (!prefixEdited) {
                    updatePrefixSuggestion();
                }
            }
        });

        shipmentBaseBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella base");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                shipmentBaseField.setText(selected.getAbsolutePath());
            }
        });

        shipmentMarkerField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!folderEdited) {
                updateFolderSuggestion();
            }
        });

        shipmentPdfField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!prefixEdited) {
                updatePrefixSuggestion();
            }
        });

        shipmentButton.setOnAction(event -> {
            Task<PdfShipmentSplitter.Result> task = new Task<>() {
                @Override
                protected PdfShipmentSplitter.Result call() throws Exception {
                    return controller.splitByShipmentCount(shipmentPdfField.getText(), shipmentMarkerField.getText(),
                            shipmentCaseSensitive.isSelected(), shipmentCountSpinner.getValue(),
                            shipmentBaseField.getText(), shipmentFolderField.getText(), shipmentPrefixField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(shipmentButton, shipmentProgress, task);
            task.setOnSucceeded(e -> {
                PdfShipmentSplitter.Result result = task.getValue();
                String message = "Split completato!\nInvii rilevati: " + result.getShipmentCount()
                        + "\nFile generati: " + result.getOutputFileCount() + "\nCartella risultati: "
                        + result.getOutputDirectory();
                FxDialogUtils.showInformation("Successo", message, owner);
            });
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante lo split per invii.", true));
            new Thread(task, "shipment-split-task").start();
        });
    }

    private void updateFolderSuggestion() {
        shipmentFolderField.setText(controller.buildDefaultShipmentSplitFolderName(shipmentMarkerField.getText()));
    }

    private void updatePrefixSuggestion() {
        shipmentPrefixField.setText(controller.buildDefaultShipmentSplitPrefix(shipmentPdfField.getText()));
    }
}
