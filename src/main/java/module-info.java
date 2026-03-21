module dev.turtywurty.veldtlauncher {
    requires javafx.controls;
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
    requires com.sun.jna;

    exports dev.turtywurty.veldtlauncher;
    opens dev.turtywurty.veldtlauncher.auth.session to com.google.gson;
}
