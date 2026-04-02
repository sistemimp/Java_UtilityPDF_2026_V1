package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxDuMergeTab {
    private static final String LAYOUT_PATH = "/ui/du_merge_tab.fxml";

    private FxDuMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxDuMergeTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Unisci DU", ex);
        }
        FxDuMergeTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Unisci DU");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
