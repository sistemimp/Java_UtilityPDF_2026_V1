package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxRisoComcolorGd9630Tab {
    private static final String LAYOUT_PATH = "/ui/riso_comcolor_gd9630_tab.fxml";

    private FxRisoComcolorGd9630Tab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxRisoComcolorGd9630Tab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Riso ComColor GD9630", ex);
        }
        FxRisoComcolorGd9630TabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Riso ComColor GD9630");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}

