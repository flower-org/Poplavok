package com.poplavok;

import com.poplavok.forms.MainForm;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Don't run this class directly, use `AppLauncher`.
 * For whatever reason, running this directly will fail with an error.
 */
public class App extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage mainStage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("forms/MainForm.fxml"));
            Parent rootNode = fxmlLoader.load();

            MainForm mainForm = fxmlLoader.getController();
            mainForm.setMainStage(mainStage);

            Scene mainScene = new Scene(rootNode, 1024, 768);

            //Close all threads when we close JavaFX windows.
            mainStage.setOnHidden(event -> {
                Platform.exit();
            });

            mainStage.setTitle("JavaFX stub");
            mainStage.setScene(mainScene);
            mainStage.setResizable(true);
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
