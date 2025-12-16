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

public final class FxRisoTabContentController {
    @FXML
    private TextField risoInputField;
    @FXML
    private Button risoBrowseButton;
    @FXML
    private TextField risoOutputField;
    @FXML
    private TextField risoRecordField;
    @FXML
    private Button risoButton;
    @FXML
    private ProgressIndicator risoProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        risoBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                risoInputField.setText(selected.getAbsolutePath());
                if (risoOutputField.getText().trim().isEmpty()) {
                    risoOutputField.setText(controller.buildDefaultRisoOptimizedName(selected.toPath()));
                }
            }
        });

        risoButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.optimizeRiso(risoInputField.getText(), risoOutputField.getText(),
                            risoRecordField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(risoButton, risoProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Conversione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'ottimizzazione Riso.", true));
            new Thread(task, "riso-task").start();
        });
    }
}
