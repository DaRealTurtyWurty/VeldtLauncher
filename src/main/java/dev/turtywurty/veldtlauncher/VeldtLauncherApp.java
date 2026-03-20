package dev.turtywurty.veldtlauncher;

import dev.turtywurty.veldtlauncher.auth.session.JsonSessionStore;
import dev.turtywurty.veldtlauncher.ui.auth.PickAccountPane;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class VeldtLauncherApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("VeldtLauncher");
        stage.initStyle(StageStyle.UNDECORATED);

        Parent root = JsonSessionStore.INSTANCE.hasLastSession()
                ? new DashboardShell()
                : new PickAccountPane();
        var scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.show();
    }
}
