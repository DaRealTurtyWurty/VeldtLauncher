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
    mainModule.set("dev.turtywurty.veldtlauncher")
    mainClass.set("dev.turtywurty.veldtlauncher.VeldtLauncherApp")
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
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("com.github.javakeyring:java-keyring:1.0.4")
    compileOnly("org.jetbrains:annotations:26.0.2")
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/veldtlauncher-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "veldtlauncher"
    }
}
