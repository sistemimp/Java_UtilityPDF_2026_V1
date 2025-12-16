package olivieri.alex.fx.tab;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import olivieri.alex.fx.PdfUtilityFxController;

import java.io.IOException;

public final class FxCsvToExcelTab {
    private static final String LAYOUT_PATH = "/ui/csv_to_excel_tab.fxml";

    private FxCsvToExcelTab() {
    }

    public static Tab create(PdfUtilityFxController controller, Window owner) {
        FXMLLoader loader = new FXMLLoader(FxCsvToExcelTab.class.getResource(LAYOUT_PATH));
        BorderPane root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Impossibile caricare la scheda CSV in Excel", ex);
        }
        FxCsvToExcelTabContentController contentController = loader.getController();
        contentController.bind(controller, owner);

        Tab tab = new Tab("CSV in Excel");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
