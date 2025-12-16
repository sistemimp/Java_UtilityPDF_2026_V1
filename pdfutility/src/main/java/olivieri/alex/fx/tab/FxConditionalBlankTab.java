package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxConditionalBlankTab {
    private static final String LAYOUT_PATH = "/ui/conditional_blank_tab.fxml";

    private FxConditionalBlankTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxConditionalBlankTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Pagine Bianche dopo testo", ex);
        }
        FxConditionalBlankTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Pagine Bianche dopo testo");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
