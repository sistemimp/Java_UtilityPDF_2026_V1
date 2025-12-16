package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxMarkerSplitTab {
    private static final String LAYOUT_PATH = "/ui/marker_split_tab.fxml";

    private FxMarkerSplitTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxMarkerSplitTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Split per stringa", ex);
        }
        FxMarkerSplitTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Split per stringa");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
