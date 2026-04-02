package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxIntervalMergeTab {
    private static final String LAYOUT_PATH = "/ui/interval_merge_tab.fxml";

    private FxIntervalMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxIntervalMergeTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Merge a blocchi", ex);
        }
        FxIntervalMergeTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Merge a blocchi");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
