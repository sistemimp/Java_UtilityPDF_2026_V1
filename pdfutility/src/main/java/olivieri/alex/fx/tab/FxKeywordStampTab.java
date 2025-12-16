package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxKeywordStampTab {
    private static final String LAYOUT_PATH = "/ui/keyword_stamp_tab.fxml";

    private FxKeywordStampTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxKeywordStampTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Timbro parole chiave", ex);
        }
        FxKeywordStampTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Timbro parole chiave");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
