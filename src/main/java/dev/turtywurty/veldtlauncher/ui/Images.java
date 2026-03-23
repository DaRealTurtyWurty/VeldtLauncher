package dev.turtywurty.veldtlauncher.ui;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.Objects;

public final class Images {
    private static final String UI_IMAGE_ROOT = "/dev/turtywurty/veldtlauncher/images/";

    private Images() {
    }

    public static String resolve(String imageName) {
        String value = Objects.requireNonNull(imageName, "imageName");
        URL resource = Images.class.getResource(UI_IMAGE_ROOT + value);
        return Objects.requireNonNull(resource, "Missing image: " + value).toExternalForm();
    }

    public static Image load(String imageName, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth) {
        return new Image(resolve(imageName), requestedWidth, requestedHeight, preserveRatio, smooth);
    }

    public static Image load(String imageName, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth,
            boolean backgroundLoading) {
        return new Image(resolve(imageName), requestedWidth, requestedHeight, preserveRatio, smooth, backgroundLoading);
    }
}
