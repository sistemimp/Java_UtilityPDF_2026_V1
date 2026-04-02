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
import olivieri.alex.util.PdfCsvRenamer;

import java.io.File;

public final class FxCsvRenameTabContentController {
    @FXML
    private TextField csvRenameDirectoryField;
    @FXML
    private Button csvRenameDirectoryBrowse;
    @FXML
    private TextField csvRenameCsvField;
    @FXML
    private Button csvRenameCsvBrowse;
    @FXML
    private Button csvRenameButton;
    @FXML
    private ProgressIndicator csvRenameProgress;
    @FXML
    private CheckBox csvRenameDirectMoveCheck;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        csvRenameDirectoryBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                csvRenameDirectoryField.setText(selected.getAbsolutePath());
            }
        });

        csvRenameCsvBrowse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona file CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                csvRenameCsvField.setText(selected.getAbsolutePath());
            }
        });

        csvRenameButton.setOnAction(event -> {
            Task<PdfCsvRenamer.Result> task = new Task<>() {
                @Override
                protected PdfCsvRenamer.Result call() throws Exception {
                    return controller.renameFromCsv(csvRenameDirectoryField.getText(), csvRenameCsvField.getText(),
                            csvRenameDirectMoveCheck.isSelected());
                }
            };
            FxTabControllerSupport.bindUiState(csvRenameButton, csvRenameProgress, task);
            task.setOnSucceeded(e -> {
                PdfCsvRenamer.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Rinomina completata!\nFile rinominati: ")
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
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante la rinomina."), owner));
            new Thread(task, "csv-rename-task").start();
        });
    }
}
