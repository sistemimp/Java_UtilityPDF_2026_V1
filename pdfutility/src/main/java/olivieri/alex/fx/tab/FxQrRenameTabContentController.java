package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfQrRenamer;

import java.io.File;

public final class FxQrRenameTabContentController {
    @FXML
    private TextField qrRenameInputField;
    @FXML
    private Button qrRenameInputBrowse;
    @FXML
    private TextField qrRenameOutputField;
    @FXML
    private Button qrRenameOutputBrowse;
    @FXML
    private Button qrRenameButton;
    @FXML
    private ProgressIndicator qrRenameProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        qrRenameInputBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF sorgente");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                qrRenameInputField.setText(selected.getAbsolutePath());
            }
        });

        qrRenameOutputBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella di output");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                qrRenameOutputField.setText(selected.getAbsolutePath());
            }
        });

        qrRenameButton.setOnAction(event -> {
            Task<PdfQrRenamer.Result> task = new Task<>() {
                @Override
                protected PdfQrRenamer.Result call() throws Exception {
                    return controller.renameByQr(qrRenameInputField.getText(), qrRenameOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(qrRenameButton, qrRenameProgress, task);
            task.setOnSucceeded(e -> {
                PdfQrRenamer.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Operazione completata!")
                        .append("\nPDF scansionati: ").append(result.getScannedCount())
                        .append("\nPDF copiati: ").append(result.getCopiedCount());
                if (result.hasWarnings()) {
                    message.append("\nAvvisi:");
                    for (String warning : result.getWarnings()) {
                        message.append("\n- ").append(warning);
                    }
                }
                FxDialogUtils.showInformation("Successo", message.toString(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(),
                            "Errore durante la rinomina con QR."), owner));
            new Thread(task, "qr-rename-task").start();
        });
    }
}
