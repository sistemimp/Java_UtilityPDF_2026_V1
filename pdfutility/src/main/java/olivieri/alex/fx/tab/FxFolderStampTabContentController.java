package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfFolderStamper;

import java.io.File;

public final class FxFolderStampTabContentController {
    @FXML
    private TextField folderStampDirectoryField;
    @FXML
    private Button folderStampBrowse;
    @FXML
    private TextField folderStampTextField;
    @FXML
    private Spinner<Double> folderStampXSpinner;
    @FXML
    private Spinner<Double> folderStampYSpinner;
    @FXML
    private Button folderStampButton;
    @FXML
    private ProgressIndicator folderStampProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        folderStampXSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));
        folderStampYSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1000, 0, 1));

        folderStampBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                folderStampDirectoryField.setText(selected.getAbsolutePath());
            }
        });

        folderStampButton.setOnAction(event -> {
            Task<PdfFolderStamper.Result> task = new Task<>() {
                @Override
                protected PdfFolderStamper.Result call() throws Exception {
                    return controller.stampFolder(folderStampDirectoryField.getText(), folderStampTextField.getText(),
                            folderStampXSpinner.getValue().floatValue(), folderStampYSpinner.getValue().floatValue());
                }
            };
            FxTabControllerSupport.bindUiState(folderStampButton, folderStampProgress, task);
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
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'applicazione del timbro."), owner));
            new Thread(task, "folder-stamp-task").start();
        });
    }
}
