package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxQrRenameTab {
    private static final String LAYOUT_PATH = "/ui/qr_rename_tab.fxml";

    private FxQrRenameTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxQrRenameTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Rinomina da QR", ex);
        }
        FxQrRenameTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Rinomina da QR");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
