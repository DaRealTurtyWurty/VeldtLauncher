package dev.turtywurty.veldtlauncher;

import dev.turtywurty.veldtlauncher.auth.session.JsonSessionStore;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import dev.turtywurty.veldtlauncher.ui.auth.PickAccountPane;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class VeldtLauncherApp extends Application {
    private static final double DEFAULT_WIDTH = 1200;
    private static final double DEFAULT_HEIGHT = 800;
    private static final double MIN_WIDTH = 960;
    private static final double MIN_HEIGHT = 640;

    @Override
    public void start(Stage stage) {
        stage.setTitle("VeldtLauncher");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        Parent root = JsonSessionStore.INSTANCE.hasLastSession()
                ? new DashboardShell()
                : new PickAccountPane();
        var scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        WindowChrome.installResizeSupport(scene);
        stage.setScene(scene);
        stage.show();
    }
}
