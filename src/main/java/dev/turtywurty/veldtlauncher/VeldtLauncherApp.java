package dev.turtywurty.veldtlauncher;

import dev.turtywurty.veldtlauncher.ui.auth.PickAccountPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class VeldtLauncherApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("VeldtLauncher");
        stage.initStyle(StageStyle.UNDECORATED);

        var scene = new Scene(new PickAccountPane(), 1200, 800);
        stage.setScene(scene);
        stage.show();
    }
}
