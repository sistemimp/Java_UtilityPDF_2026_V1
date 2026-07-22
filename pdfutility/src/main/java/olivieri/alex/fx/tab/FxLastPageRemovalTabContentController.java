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
import olivieri.alex.util.PdfLastPageRemover;

import java.io.File;

public final class FxLastPageRemovalTabContentController {
    @FXML
    private TextField lastPageDirectoryField;
    @FXML
    private Button lastPageBrowseButton;
    @FXML
    private Button lastPageProcessButton;
    @FXML
    private ProgressIndicator lastPageProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        lastPageBrowseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Seleziona cartella PDF");
            File selected = chooser.showDialog(owner);
            if (selected != null) {
                lastPageDirectoryField.setText(selected.getAbsolutePath());
            }
        });

        lastPageProcessButton.setOnAction(event -> {
            Task<PdfLastPageRemover.Result> task = new Task<>() {
                @Override
                protected PdfLastPageRemover.Result call() throws Exception {
                    return controller.removeLastPagesFromDirectory(lastPageDirectoryField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(lastPageProcessButton, lastPageProgress, task);
            task.setOnSucceeded(e -> {
                PdfLastPageRemover.Result result = task.getValue();
                StringBuilder message = new StringBuilder("Operazione completata!\nFile elaborati: ")
                        .append(result.getProcessedCount())
                        .append("\nCartella output: ")
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
                    FxTabControllerSupport.getRootCauseMessage(task.getException(),
                            "Errore durante la rimozione dell'ultima pagina."),
                    owner));
            new Thread(task, "last-page-removal-task").start();
        });
    }
}
