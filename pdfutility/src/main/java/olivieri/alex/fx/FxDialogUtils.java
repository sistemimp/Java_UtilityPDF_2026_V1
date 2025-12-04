package olivieri.alex.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Helpers to keep dialog creation consistent between tabs.
 */
public final class FxDialogUtils {
    private FxDialogUtils() {
    }

    public static void showInformation(String title, String message, Window owner) {
        showAlert(Alert.AlertType.INFORMATION, title, null, message, owner);
    }

    public static void showWarning(String title, String message, Window owner) {
        showAlert(Alert.AlertType.WARNING, title, null, message, owner);
    }

    public static void showError(String title, String message, Window owner) {
        showAlert(Alert.AlertType.ERROR, title, null, message, owner);
    }

    public static ButtonType showConfirmation(String title, String message, Window owner) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, null, message, owner);
        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    private static void showAlert(Alert.AlertType type, String title, String header, String content, Window owner) {
        Alert alert = createAlert(type, title, header, content, owner);
        alert.showAndWait();
    }

    private static Alert createAlert(Alert.AlertType type, String title, String header, String content, Window owner) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert;
    }
}
