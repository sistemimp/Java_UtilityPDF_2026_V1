package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxPageFilterTab {
    private static final String LAYOUT_PATH = "/ui/page_filter_tab.fxml";

    private FxPageFilterTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxPageFilterTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Filtro pagine", ex);
        }
        FxPageFilterTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Filtro pagine");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
