package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxShipmentSplitTab {
    private static final String LAYOUT_PATH = "/ui/shipment_split_tab.fxml";

    private FxShipmentSplitTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxShipmentSplitTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Split per invii", ex);
        }
        FxShipmentSplitTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Split per invii");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
