package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxPairMergeTab {
    private static final String LAYOUT_PATH = "/ui/pair_merge_tab.fxml";

    private FxPairMergeTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxPairMergeTab.class.getResource(LAYOUT_PATH));
        BorderPane content;
        try {
            content = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Unisci per nome", ex);
        }
        FxPairMergeTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Unisci per nome");
        tab.setClosable(false);
        tab.setContent(content);
        return tab;
    }
}
