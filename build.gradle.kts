plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "2.0.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.2.1"
}

group = "dev.turtywurty"
version = "1.0.0"

repositories {
    mavenCentral()
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("dev.turtywurty.minecraftlauncher")
    mainClass.set("dev.turtywurty.minecraftlauncher.MinecraftLauncherApp")
}

javafx {
    version = "25.0.2"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-fontawesome6-pack:12.4.0")
    implementation("io.javalin:javalin:7.1.0")
    implementation("com.google.code.gson:gson:2.13.2")
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}
