package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;
import olivieri.alex.util.PdfPairMerger;

import java.io.File;

public final class FxPairMergeTabContentController {
    @FXML
    private TextField pairFirstField;
    @FXML
    private Button pairFirstBrowse;
    @FXML
    private TextField pairSecondField;
    @FXML
    private Button pairSecondBrowse;
    @FXML
    private Button pairMergeButton;
    @FXML
    private CheckBox normalizePairCheck;
    @FXML
    private ProgressIndicator pairProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        pairFirstBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona prima cartella");
            chooser.setInitialDirectory(null);
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                pairFirstField.setText(selected.getAbsolutePath());
            }
        });
        pairSecondBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona seconda cartella");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                pairSecondField.setText(selected.getAbsolutePath());
            }
        });

        pairMergeButton.setOnAction(event -> {
            Task<PdfPairMerger.Result> task = new Task<>() {
                @Override
                protected PdfPairMerger.Result call() throws Exception {
                    boolean normalizeFR = normalizePairCheck != null && normalizePairCheck.isSelected();
                    return controller.mergeMatchingPairs(pairFirstField.getText(), pairSecondField.getText(), normalizeFR);
                }
            };
            FxTabControllerSupport.bindUiState(pairMergeButton, pairProgress, task);
            task.setOnSucceeded(e -> showSuccess(task.getValue()));
            task.setOnFailed(e -> FxDialogUtils.showError("Errore",
                    FxTabControllerSupport.getRootCauseMessage(task.getException(), "Errore durante l'unione per nome."), owner));
            new Thread(task, "pair-merge-task").start();
        });
    }

    private void showSuccess(PdfPairMerger.Result result) {
        StringBuilder message = new StringBuilder("Unione completata!\nFile generati: ")
                .append(result.getMergedCount()).append("\nCartella risultante: ").append(result.getOutputDirectory());
        if (result.hasMissingReport()) {
            message.append("\nFile mancanti: ").append(result.getMissingReport());
        }
        FxDialogUtils.showInformation("Successo", message.toString(), owner);
    }
}
