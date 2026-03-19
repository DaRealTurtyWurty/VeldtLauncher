package dev.turtywurty.veldtlauncher.ui;

import javafx.scene.Parent;

import java.net.URL;
import java.util.Objects;

public final class Stylesheets {
    private static final String UI_STYLESHEET_ROOT = "/dev/turtywurty/veldtlauncher/styles/";

    private Stylesheets() {
    }

    public static void add(Parent parent, String stylesheetName) {
        Objects.requireNonNull(parent, "parent");
        parent.getStylesheets().add(resolve(stylesheetName));
    }

    public static void addAll(Parent parent, String... stylesheetNames) {
        Objects.requireNonNull(stylesheetNames, "stylesheetNames");
        for (String stylesheetName : stylesheetNames) {
            add(parent, stylesheetName);
        }
    }

    public static String resolve(String stylesheetName) {
        String value = Objects.requireNonNull(stylesheetName, "stylesheetName");
        URL resource = Stylesheets.class.getResource(UI_STYLESHEET_ROOT + value);
        return Objects.requireNonNull(resource, "Missing stylesheet: " + value).toExternalForm();
    }
}
