package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxAlternatingMixTab {
    private static final String LAYOUT_PATH = "/ui/alternating_mix_tab.fxml";

    private FxAlternatingMixTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxAlternatingMixTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Miscelazione alternata", ex);
        }
        FxAlternatingMixTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Miscelazione alternata");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
