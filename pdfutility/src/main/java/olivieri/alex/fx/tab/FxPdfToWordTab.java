package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxPdfToWordTab {
    private static final String LAYOUT_PATH = "/ui/pdf_to_word_tab.fxml";

    private FxPdfToWordTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxPdfToWordTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda PDF in Word", ex);
        }
        FxPdfToWordTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("PDF in Word");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
