package dev.turtywurty.veldtlauncher.instance.play;

import dev.turtywurty.veldtlauncher.auth.session.JsonSessionStore;
import dev.turtywurty.veldtlauncher.auth.session.MinecraftSession;
import dev.turtywurty.veldtlauncher.auth.session.StoredMinecraftSessionService;
import dev.turtywurty.veldtlauncher.auth.session.StoredSessionMetadata;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.instance.JsonInstanceStore;
import dev.turtywurty.veldtlauncher.instance.StoredInstanceMetadata;
import dev.turtywurty.veldtlauncher.instance.StoredVanillaInstanceMetadata;
import dev.turtywurty.veldtlauncher.minecraft.install.DefaultInstalledVersionService;
import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.install.VanillaVersionInstallService;
import dev.turtywurty.veldtlauncher.minecraft.java.DefaultJavaDiscoveryService;
import dev.turtywurty.veldtlauncher.minecraft.launch.DefaultGameLaunchService;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.DefaultProcessMonitor;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessMonitorHandle;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessOutputLine;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessExitEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessMonitoringFailedEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event.ProcessOutputEvent;
import dev.turtywurty.veldtlauncher.minecraft.launch.args.DefaultLaunchArgumentBuilder;
import dev.turtywurty.veldtlauncher.minecraft.manifest.MojangVersionManifestService;
import dev.turtywurty.veldtlauncher.minecraft.mapping.Mappings;
import dev.turtywurty.veldtlauncher.minecraft.mapping.MojangMappingsService;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public final class InstancePlayService {
    public static final InstancePlayService INSTANCE = new InstancePlayService();
    private static final Logger LOGGER = LoggerFactory.getLogger(InstancePlayService.class);

    private InstancePlayService() {
    }

    public void start(StoredInstanceMetadata instance, EventStream eventStream) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(eventStream, "eventStream");

        InstancePlayReporter reporter = new InstancePlayReporter(eventStream);
        Thread.ofPlatform().name("Instance Play Thread").start(() -> play(instance, reporter));
    }

    private void play(StoredInstanceMetadata instance, InstancePlayReporter reporter) {
        try {
            switch (instance) {
                case StoredVanillaInstanceMetadata vanillaInstance -> playVanilla(vanillaInstance, reporter);
                default -> throw new UnsupportedOperationException(
                        "Play is currently only implemented for vanilla instances.");
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to play instance {}", instance.id(), exception);
            String message = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "Failed to play instance."
                    : exception.getMessage();
            reporter.failed(reporter.currentStep(), message, exception);
        }
    }

    private void playVanilla(StoredVanillaInstanceMetadata instance, InstancePlayReporter reporter) throws Exception {
        String versionId = requireVersionId(instance);
        Path gameDirectory = instance.gameDirectory();
        if (gameDirectory == null)
            throw new IllegalStateException("This instance does not have a game directory configured.");

        var versionManifestService = new MojangVersionManifestService(gameDirectory);

        reporter.progress(InstancePlayStep.AUTHENTICATING, "Refreshing Minecraft session", 0D);
        StoredSessionMetadata storedSession = JsonSessionStore.INSTANCE.load()
                .orElseThrow(() -> new IllegalStateException("No signed-in Minecraft account is available."));
        MinecraftSession session = StoredMinecraftSessionService.INSTANCE.require(storedSession.userId());
        reporter.progress(InstancePlayStep.AUTHENTICATING, "Minecraft session refreshed", 1D);

        reporter.progress(InstancePlayStep.FETCHING_METADATA, "Fetching version metadata for " + versionId, 0D);
        VersionMetadata versionMetadata = versionManifestService.fetchVersionMetadata(versionId);
        if (versionMetadata == null)
            throw new IllegalStateException("Failed to fetch version metadata for " + versionId + ".");
        reporter.progress(InstancePlayStep.FETCHING_METADATA, "Version metadata loaded", 1D);

        InstallResult installResult = new VanillaVersionInstallService(versionManifestService, reporter)
                .install(versionMetadata, gameDirectory);
        if (installResult == null)
            throw new IllegalStateException("Version installation did not return a result.");

        if (installResult.isPartiallyInstalled())
            throw new IllegalStateException("Version installation is incomplete: " + installResult.getPartiallyInstalledReason());

        reporter.progress(InstancePlayStep.LAUNCHING, "Preparing launch command", 0D);
        Process process = new DefaultGameLaunchService(
                new DefaultInstalledVersionService(),
                new DefaultJavaDiscoveryService(),
                new DefaultLaunchArgumentBuilder())
                .launch(versionMetadata, installResult, session);
        if (process == null)
            throw new IllegalStateException("Minecraft launch did not return a process.");

        JsonInstanceStore.INSTANCE.setLastPlayed(instance.id());
        ProcessMonitorHandle monitorHandle = attachMonitor(process, reporter);
        Mappings mappings = MojangMappingsService.INSTANCE.loadOrEmpty(gameDirectory, versionMetadata.id());
        reporter.log(InstancePlayStep.LAUNCHING, "Minecraft process started.");
        reporter.progress(InstancePlayStep.LAUNCHING, "Minecraft launched", 1D);
        reporter.completed("Minecraft started.", monitorHandle, buildLogsWindowTitle(instance, versionMetadata), mappings);
    }

    private ProcessMonitorHandle attachMonitor(Process process, InstancePlayReporter reporter) {
        ProcessMonitorHandle monitorHandle = new DefaultProcessMonitor().attach(process);
        monitorHandle.eventStream().registerListener(ProcessOutputEvent.class, event -> {
            ProcessOutputLine line = event.line();
            if (line.streamType() == ProcessOutputLine.StreamType.STDERR) {
                reporter.error(InstancePlayStep.LAUNCHING, line.line());
            } else {
                reporter.log(InstancePlayStep.LAUNCHING, line.line());
            }
        });
        monitorHandle.eventStream().registerListener(ProcessMonitoringFailedEvent.class, event -> {
            Throwable cause = event.cause();
            String message = cause == null || cause.getMessage() == null || cause.getMessage().isBlank()
                    ? "Process monitoring failed."
                    : "Process monitoring failed: " + cause.getMessage();
            reporter.error(InstancePlayStep.LAUNCHING, message);
        });
        monitorHandle.eventStream().registerListener(ProcessExitEvent.class, event -> {
            int exitCode = event.exitCode();
            if (exitCode == 0) {
                reporter.log(InstancePlayStep.LAUNCHING, "Minecraft exited with code 0.");
            } else {
                reporter.error(InstancePlayStep.LAUNCHING, "Minecraft exited with code " + exitCode + ".");
            }

            monitorHandle.stopMonitoring();
        });
        return monitorHandle;
    }

    private String buildLogsWindowTitle(StoredVanillaInstanceMetadata instance, VersionMetadata versionMetadata) {
        String name = instance.name();
        if (name != null && !name.isBlank())
            return "Minecraft Logs - " + name;

        return "Minecraft Logs - " + versionMetadata.id();
    }

    private String requireVersionId(StoredVanillaInstanceMetadata instance) {
        String versionId = instance.minecraftVersion();
        if (versionId == null || versionId.isBlank())
            throw new IllegalStateException("This vanilla instance does not have a Minecraft version configured.");

        return versionId;
    }
}
