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

public final class FxRisoComcolorGd9630TabContentController {
    @FXML
    private TextField risoGdInputField;
    @FXML
    private Button risoGdBrowseButton;
    @FXML
    private TextField risoGdOutputField;
    @FXML
    private TextField risoGdRecordField;
    @FXML
    private Button risoGdButton;
    @FXML
    private ProgressIndicator risoGdProgress;

    private PdfUtilityFxController controller;
    private Window owner;

    void bind(PdfUtilityFxController controller, Window owner) {
        this.controller = controller;
        this.owner = owner;

        risoGdBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleziona PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File selected = chooser.showOpenDialog(owner);
            if (selected != null) {
                risoGdInputField.setText(selected.getAbsolutePath());
                if (risoGdOutputField.getText().trim().isEmpty()) {
                    risoGdOutputField.setText(controller.buildDefaultRisoComcolorGd9630OptimizedName(selected.toPath()));
                }
            }
        });

        risoGdButton.setOnAction(event -> {
            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    return controller.optimizeRisoComcolorGd9630(risoGdInputField.getText(), risoGdOutputField.getText(),
                            risoGdRecordField.getText());
                }
            };
            FxTabControllerSupport.bindUiState(risoGdButton, risoGdProgress, task);
            task.setOnSucceeded(e -> FxDialogUtils.showInformation("Successo",
                    "Conversione completata!\nFile creato: " + task.getValue(), owner));
            task.setOnFailed(e -> FxTabControllerSupport.showFailure(owner, task.getException(),
                    "Errore durante l'ottimizzazione Riso ComColor GD9630.", true));
            new Thread(task, "riso-gd9630-task").start();
        });
    }
}

