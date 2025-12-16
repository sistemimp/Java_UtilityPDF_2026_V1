package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxCsvTxtMergeTab {
    private static final String LAYOUT_PATH = "/ui/csv_txt_merge_tab.fxml";

    private FxCsvTxtMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxCsvTxtMergeTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Unisci CSV/TXT", ex);
        }
        FxCsvTxtMergeTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Unisci CSV/TXT");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
