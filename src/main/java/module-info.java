module dev.turtywurty.veldtlauncher {
    requires javafx.controls;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;

    requires io.javalin;
    requires java.desktop;
    requires java.net.http;
    requires static org.jetbrains.annotations;
    requires com.google.gson;
    requires java.keyring;
    requires io.nayuki.qrcodegen;
    requires javafx.swing;

    exports dev.turtywurty.veldtlauncher;
    opens dev.turtywurty.veldtlauncher.auth.session to com.google.gson;
    opens dev.turtywurty.veldtlauncher.instance to com.google.gson;
    opens dev.turtywurty.veldtlauncher.minecraft.metadata to com.google.gson;
    opens dev.turtywurty.veldtlauncher.minecraft.metadata.model to com.google.gson;
    opens dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument to com.google.gson;
    opens dev.turtywurty.veldtlauncher.minecraft.metadata.model.download to com.google.gson;
    opens dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging to com.google.gson;
}
