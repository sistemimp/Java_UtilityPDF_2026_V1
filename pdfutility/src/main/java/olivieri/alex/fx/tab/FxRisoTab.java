package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxRisoTab {
    private static final String LAYOUT_PATH = "/ui/riso_tab.fxml";

    private FxRisoTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxRisoTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Riso GL9730", ex);
        }
        FxRisoTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Riso GL9730");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
