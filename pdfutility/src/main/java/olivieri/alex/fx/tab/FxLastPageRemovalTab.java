package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxLastPageRemovalTab {
    private static final String LAYOUT_PATH = "/ui/last_page_removal_tab.fxml";

    private FxLastPageRemovalTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxLastPageRemovalTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Rimuovi ultima pagina", ex);
        }
        FxLastPageRemovalTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Rimuovi ultima pagina");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
