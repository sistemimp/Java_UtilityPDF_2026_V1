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
import olivieri.alex.util.FolderProgressiveCreator;

import java.io.File;

public final class FxFolderProgressiveCreatorTabContentController {
    @FXML
    private TextField folderProgressiveBaseDirectoryField;
    @FXML
    private Button folderProgressiveBaseDirectoryBrowse;
    @FXML
    private TextField folderProgressivePrefixField;
    @FXML
    private Spinner<Integer> folderProgressiveCountSpinner;
    @FXML
    private Button folderProgressiveCreateButton;
    @FXML
    private ProgressIndicator folderProgressiveProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        folderProgressiveBaseDirectoryBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella di destinazione");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                folderProgressiveBaseDirectoryField.setText(selected.getAbsolutePath());
            }
        });

        folderProgressiveCreateButton.setOnAction(event -> {
            Task<FolderProgressiveCreator.Result> task = new Task<>() {
                @Override
                protected FolderProgressiveCreator.Result call() throws Exception {
                    Integer countValue = folderProgressiveCountSpinner.getValue();
                    int count = countValue == null ? 1 : countValue;
                    return controller.createProgressiveFolders(
                            folderProgressiveBaseDirectoryField.getText(),
                            folderProgressivePrefixField.getText(),
                            count);
                }
            };
            FxTabControllerSupport.bindUiState(folderProgressiveCreateButton, folderProgressiveProgress, task);
            task.setOnSucceeded(e -> {
                FolderProgressiveCreator.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Creazione cartelle completata!\nCartelle create: ")
                        .append(result.getCreatedCount())
                        .append("\nCartelle saltate: ")
                        .append(result.getSkippedCount())
                        .append("\nDestinazione: ")
                        .append(result.getBaseDirectory().toAbsolutePath());
                if (result.hasWarnings()) {
                    message.append("\nAvvisi:");
                    for (String warning : result.getWarnings()) {
                        message.append("\n- ").append(warning);
                    }
                }
                FxDialogUtils.showInformation("Successo", message.toString(), owner);
            });
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante la creazione delle cartelle.", true));
            new Thread(task, "folder-progressive-create-task").start();
        });
    }
}
