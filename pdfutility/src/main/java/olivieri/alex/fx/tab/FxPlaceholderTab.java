package olivieri.alex.fx.tab;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

public final class FxPlaceholderTab {
    private FxPlaceholderTab() {
    }

    public static Tab create(String title, String description) {
        Tab tab = new Tab(title);
        tab.setClosable(false);
        Label message = new Label(description);
        message.setWrapText(true);
        BorderPane pane = new BorderPane(message);
        pane.setPadding(new Insets(20));
        tab.setContent(pane);
        return tab;
    }
}
