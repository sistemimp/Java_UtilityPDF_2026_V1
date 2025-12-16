package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxMergeTab {
    private static final String LAYOUT_PATH = "/ui/merge_tab.fxml";

    private FxMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxMergeTab.class.getResource(LAYOUT_PATH));
        BorderPane content;
        try {
            content = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Accoda PDF", ex);
        }
        FxMergeTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Unione PDF");
        tab.setClosable(false);
        tab.setContent(content);
        return tab;
    }
}
