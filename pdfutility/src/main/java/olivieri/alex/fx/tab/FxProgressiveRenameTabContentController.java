package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfProgressiveRenamer;

import java.io.File;

public final class FxProgressiveRenameTabContentController {
    @FXML
    private TextField progressiveRenameDirectoryField;
    @FXML
    private Button progressiveRenameDirectoryBrowse;
    @FXML
    private Spinner<Integer> progressiveRenameWidthSpinner;
    @FXML
    private Button progressiveRenameButton;
    @FXML
    private ProgressIndicator progressiveRenameProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        progressiveRenameDirectoryBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                progressiveRenameDirectoryField.setText(selected.getAbsolutePath());
            }
        });

        progressiveRenameButton.setOnAction(event -> {
            Task<PdfProgressiveRenamer.Result> task = new Task<>() {
                @Override
                protected PdfProgressiveRenamer.Result call() throws Exception {
                    Integer widthValue = progressiveRenameWidthSpinner.getValue();
                    int width = widthValue == null ? 3 : widthValue;
                    return controller.renameProgressive(progressiveRenameDirectoryField.getText(), width);
                }
            };
            FxTabControllerSupport.bindUiState(progressiveRenameButton, progressiveRenameProgress, task);
            task.setOnSucceeded(e -> {
                PdfProgressiveRenamer.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Rinomina progressiva completata!\nFile rinominati: ")
                        .append(result.getRenamedCount());
                if (result.hasWarnings()) {
                    message.append("\nAvvisi:");
                    for (String warning : result.getWarnings()) {
                        message.append("\n- ").append(warning);
                    }
                }
                FxDialogUtils.showInformation("Successo", message.toString(), owner);
            });
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante la rinomina."),
                    owner));
            new Thread(task, "progressive-rename-task").start();
        });
    }
}
