package olivieri.alex.fx.tab;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Window;
import olivieri.alex.fx.FxDialogUtils;

final class FxTabControllerSupport {
    private FxTabControllerSupport() {
    }

    static void bindUiState(Button actionButton, ProgressIndicator indicator, Task<?> task) {
        actionButton.disableProperty().bind(task.runningProperty());
        indicator.visibleProperty().bind(task.runningProperty());
    }

    static String getRootCauseMessage(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null && !message.isEmpty() ? message : fallback;
    }

    static void showFailure(Window owner, Throwable throwable, String fallbackMessage, boolean warnOnIllegalArgument) {
        if (owner == null) {
            return;
        }
        Throwable root = throwable;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        String message = root != null ? root.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            message = fallbackMessage;
        }
        if (warnOnIllegalArgument && root instanceof IllegalArgumentException) {
            FxDialogUtils.showWarning("Attenzione", message, owner);
        } else {
            FxDialogUtils.showError("Errore", message, owner);
        }
    }
}
