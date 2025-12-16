package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.File;
import java.nio.file.Path;

public final class FxBlankPagesTabContentController {
    @FXML
    private TextField blankInputField;
    @FXML
    private Button blankBrowseButton;
    @FXML
    private TextField blankOutputField;
    @FXML
    private Button blankProcessButton;
    @FXML
    private ProgressIndicator blankProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        blankBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                blankInputField.setText(selected.getAbsolutePath());
                Path pdfPath = selected.toPath();
                if (blankOutputField.getText().trim().isEmpty()) {
                    blankOutputField.setText(controller.buildDefaultBlankPagesName(pdfPath));
                }
            }
        });

        blankProcessButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.insertBlankPages(blankInputField.getText(), blankOutputField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(blankProcessButton, blankProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Operazione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'elaborazione.", false));
            new Thread(task, "blank-pages-task").start();
        });
    }
}
