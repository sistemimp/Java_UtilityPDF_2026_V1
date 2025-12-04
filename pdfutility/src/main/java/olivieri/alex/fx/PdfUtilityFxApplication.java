package olivieri.alex.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public final class PdfUtilityFxApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/PdfUtilityLayout.fxml"));
            BorderPane root = loader.load();
            PdfUtilityFxLayoutController layoutController = loader.getController();
            layoutController.setStage(stage);

            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            stage.setTitle("Utility PDF");
            stage.show();
        } catch (IOException ex) {
            throw new RuntimeException("Impossibile caricare la UI", ex);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
