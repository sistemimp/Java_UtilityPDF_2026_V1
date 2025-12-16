package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxFolderStampTab {
    private static final String LAYOUT_PATH = "/ui/folder_stamp_tab.fxml";

    private FxFolderStampTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxFolderStampTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Timbro cartella", ex);
        }
        FxFolderStampTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Timbro cartella");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
