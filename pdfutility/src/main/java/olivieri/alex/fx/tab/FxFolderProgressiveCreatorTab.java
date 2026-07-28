package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxFolderProgressiveCreatorTab {
    private static final String LAYOUT_PATH = "/ui/folder_progressive_creator_tab.fxml";

    private FxFolderProgressiveCreatorTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxFolderProgressiveCreatorTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Crea cartelle", ex);
        }
        FxFolderProgressiveCreatorTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Crea cartelle");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
