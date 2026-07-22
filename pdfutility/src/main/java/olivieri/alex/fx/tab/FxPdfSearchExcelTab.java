package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxPdfSearchExcelTab {
    private static final String LAYOUT_PATH = "/ui/pdf_search_excel_tab.fxml";

    private FxPdfSearchExcelTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxPdfSearchExcelTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda Estrai PDF in Excel", ex);
        }
        FxPdfSearchExcelTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("Estrai PDF");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
