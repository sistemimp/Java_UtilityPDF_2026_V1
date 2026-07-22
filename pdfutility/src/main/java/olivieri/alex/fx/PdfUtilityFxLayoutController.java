package olivieri.alex.fx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.stage.Stage;
import olivieri.alex.fx.tab.FxAlternatingMixTab;
import olivieri.alex.fx.tab.FxBlankPagesTab;
import olivieri.alex.fx.tab.FxConditionalBlankTab;
import olivieri.alex.fx.tab.FxCsvRenameTab;
import olivieri.alex.fx.tab.FxCsvToExcelTab;
import olivieri.alex.fx.tab.FxCsvTxtMergeTab;
import olivieri.alex.fx.tab.FxDuMergeTab;
import olivieri.alex.fx.tab.FxFolderStampTab;
import olivieri.alex.fx.tab.FxIntervalMergeTab;
import olivieri.alex.fx.tab.FxKeywordStampTab;
import olivieri.alex.fx.tab.FxLastPageRemovalTab;
import olivieri.alex.fx.tab.FxMarkerSplitTab;
import olivieri.alex.fx.tab.FxMergeTab;
import olivieri.alex.fx.tab.FxOptimizeTab;
import olivieri.alex.fx.tab.FxPairMergeTab;
import olivieri.alex.fx.tab.FxPdfToWordTab;
import olivieri.alex.fx.tab.FxPdfSearchExcelTab;
import olivieri.alex.fx.tab.FxPageFilterTab;
import olivieri.alex.fx.tab.FxProgressiveRenameTab;
import olivieri.alex.fx.tab.FxQrRenameTab;
import olivieri.alex.fx.tab.FxRepeatTab;
import olivieri.alex.fx.tab.FxRisoComcolorGd9630Tab;
import olivieri.alex.fx.tab.FxRisoTab;
import olivieri.alex.fx.tab.FxShipmentSplitTab;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class PdfUtilityFxLayoutController {
    @FXML
    private Tab mergeTab;
    @FXML
    private Tab intervalMergeTab;
    @FXML
    private Tab optimizeTab;
    @FXML
    private Tab risoTab;
    @FXML
    private Tab risoGd9630Tab;
    @FXML
    private Tab blankPagesTab;
    @FXML
    private Tab conditionalBlankTab;
    @FXML
    private Tab repeatTab;
    @FXML
    private Tab alternatingMixTab;
    @FXML
    private Tab pageFilterTab;
    @FXML
    private Tab lastPageRemovalTab;
    @FXML
    private Tab pairMergeTab;
    @FXML
    private Tab markerSplitTab;
    @FXML
    private Tab shipmentSplitTab;
    @FXML
    private Tab csvRenameTab;
    @FXML
    private Tab progressiveRenameTab;
    @FXML
    private Tab qrMainTab;
    @FXML
    private Tab csvToExcelTab;
    @FXML
    private Tab pdfSearchExcelTab;
    @FXML
    private Tab csvTxtMergeTab;
    @FXML
    private Tab duMergeTab;
    @FXML
    private Tab folderStampTab;
    @FXML
    private Tab keywordStampTab;
    @FXML
    private Tab pdfToWordTab;
    @FXML
    private Button openAuditLogButton;
    @FXML
    private Label appTitleLabel;

    private final PdfUtilityFxController controller = new PdfUtilityFxController();
    private Stage ownerStage;
    private boolean tabsInitialized;

    public void setStage(Stage stage) {
        this.ownerStage = stage;
        initializeTabs();
    }

    @FXML
    private void initialize() {
        openAuditLogButton.setOnAction(event -> openAuditLog());
        appTitleLabel.setText(buildApplicationTitle());
    }

    private void initializeTabs() {
        if (tabsInitialized || ownerStage == null) {
            return;
        }
        populateTab(mergeTab, FxMergeTab.create(controller, ownerStage));
        populateTab(intervalMergeTab, FxIntervalMergeTab.create(controller, ownerStage));
        populateTab(optimizeTab, FxOptimizeTab.create(controller, ownerStage));
        populateTab(risoTab, FxRisoTab.create(controller, ownerStage));
        populateTab(risoGd9630Tab, FxRisoComcolorGd9630Tab.create(controller, ownerStage));
        populateTab(blankPagesTab, FxBlankPagesTab.create(controller, ownerStage));
        populateTab(conditionalBlankTab, FxConditionalBlankTab.create(controller, ownerStage));
        populateTab(repeatTab, FxRepeatTab.create(controller, ownerStage));
        populateTab(alternatingMixTab, FxAlternatingMixTab.create(controller, ownerStage));
        populateTab(pageFilterTab, FxPageFilterTab.create(controller, ownerStage));
        populateTab(lastPageRemovalTab, FxLastPageRemovalTab.create(controller, ownerStage));
        populateTab(pairMergeTab, FxPairMergeTab.create(controller, ownerStage));
        populateTab(markerSplitTab, FxMarkerSplitTab.create(controller, ownerStage));
        populateTab(shipmentSplitTab, FxShipmentSplitTab.create(controller, ownerStage));
        populateTab(csvRenameTab, FxCsvRenameTab.create(controller, ownerStage));
        populateTab(progressiveRenameTab, FxProgressiveRenameTab.create(controller, ownerStage));
        populateTab(qrMainTab, FxQrRenameTab.create(controller, ownerStage));
        populateTab(csvToExcelTab, FxCsvToExcelTab.create(controller, ownerStage));
        populateTab(pdfSearchExcelTab, FxPdfSearchExcelTab.create(controller, ownerStage));
        populateTab(csvTxtMergeTab, FxCsvTxtMergeTab.create(controller, ownerStage));
        populateTab(duMergeTab, FxDuMergeTab.create(controller, ownerStage));
        populateTab(folderStampTab, FxFolderStampTab.create(controller, ownerStage));
        populateTab(keywordStampTab, FxKeywordStampTab.create(controller, ownerStage));
        populateTab(pdfToWordTab, FxPdfToWordTab.create(controller, ownerStage));
        tabsInitialized = true;
    }

    private void populateTab(Tab target, Tab source) {
        if (target == null || source == null) {
            return;
        }
        target.setContent(source.getContent());
        target.setClosable(false);
        target.setGraphic(source.getGraphic());
        target.setDisable(source.isDisable());
    }

    private void openAuditLog() {
        try {
            controller.openAuditLog();
            FxDialogUtils.showInformation("Log audit", "Log aperto correttamente.", ownerStage);
        } catch (UnsupportedOperationException ex) {
            FxDialogUtils.showInformation("Informazione", ex.getMessage(), ownerStage);
        } catch (IOException ex) {
            FxDialogUtils.showError("Errore", "Impossibile aprire il log: " + ex.getMessage(), ownerStage);
        }
    }

    private String buildApplicationTitle() {
        String version = resolveApplicationVersion();
        return "Utility PDF - 2026 - V" + version;
    }

    private String resolveApplicationVersion() {
        String fromManifest = getClass().getPackage().getImplementationVersion();
        if (isPresent(fromManifest)) {
            return fromManifest.trim();
        }

        try (InputStream stream = getClass()
                .getResourceAsStream("/META-INF/maven/olivieri.alex/PDFUtility_2026_V1/pom.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String fromPom = properties.getProperty("version");
                if (isPresent(fromPom)) {
                    return fromPom.trim();
                }
            }
        } catch (Exception ignored) {
            // Keep UI initialization resilient when metadata is unavailable.
        }

        return "dev";
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
