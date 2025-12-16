package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxBlankPagesTab {
    private static final String LAYOUT_PATH = "/ui/blank_pages_tab.fxml";

    private FxBlankPagesTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxBlankPagesTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Pagine Bianche", ex);
        }
        FxBlankPagesTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Pagine Bianche");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
