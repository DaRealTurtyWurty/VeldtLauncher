package dev.turtywurty.veldtlauncher.minecraft.launch;

import dev.turtywurty.veldtlauncher.auth.session.MinecraftSession;
import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.install.InstalledVersionService;
import dev.turtywurty.veldtlauncher.minecraft.java.JDK;
import dev.turtywurty.veldtlauncher.minecraft.java.JDKManager;
import dev.turtywurty.veldtlauncher.minecraft.java.JavaDiscoveryService;
import dev.turtywurty.veldtlauncher.minecraft.launch.args.LaunchArgumentBuilder;
import dev.turtywurty.veldtlauncher.minecraft.launch.args.LaunchArguments;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

public class DefaultGameLaunchService implements GameLaunchService {
    private final InstalledVersionService installedVersionService;
    private final JavaDiscoveryService javaDiscoveryService;
    private final LaunchArgumentBuilder launchArgumentBuilder;

    public DefaultGameLaunchService(
            InstalledVersionService installedVersionService,
            JavaDiscoveryService javaDiscoveryService,
            LaunchArgumentBuilder launchArgumentBuilder
    ) {
        this.installedVersionService = installedVersionService;
        this.javaDiscoveryService = javaDiscoveryService;
        this.launchArgumentBuilder = launchArgumentBuilder;
    }

    @Override
    public Process launch(VersionMetadata metadata, InstallResult installResult, MinecraftSession session) throws IOException {
        if (installResult == null || installResult.gameDirectory() == null)
            throw new IllegalArgumentException("Install result must include a game directory.");

        Path gameDirectory = installResult.gameDirectory();
        InstallResult installed = installedVersionService.isInstalled(metadata.id(), gameDirectory);
        if (installed == null)
            throw new IllegalStateException("Version is not installed.");

        if (installed.isPartiallyInstalled())
            throw new IllegalStateException("Version is only partially installed. " + installed.getPartiallyInstalledReason());

        if (metadata.mainClass() == null || metadata.mainClass().isBlank())
            throw new IllegalStateException("Version metadata is missing the main class.");

        Optional<JDK> jdkOpt = javaDiscoveryService.findJavaExecutable(metadata.javaVersion());
        if (jdkOpt.isEmpty())
            throw new IllegalStateException("No suitable Java installation found for version " + metadata.javaVersion());

        JDK jdk = jdkOpt.get();
        Path javaExecutable = jdk.executablePath(JDKManager.JAVA_EXECUTABLE_NAME);
        if (Files.notExists(javaExecutable))
            throw new IllegalStateException("Java executable does not exist at path: " + javaExecutable);

        if (!Files.isRegularFile(javaExecutable))
            throw new IllegalStateException("Java executable is a directory: " + javaExecutable);

        if (!Files.isExecutable(javaExecutable))
            throw new IllegalStateException("Java executable is not executable: " + javaExecutable);

        LaunchArguments launchArgs = launchArgumentBuilder.build(metadata, installed, session, jdk);
        var command = new ArrayList<String>();
        command.add(javaExecutable.toString());
        command.addAll(launchArgs.jvmArguments());
        command.add(metadata.mainClass());
        command.addAll(launchArgs.gameArguments());

        var processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        processBuilder.directory(gameDirectory.toFile());

        return processBuilder.start();
    }
}
