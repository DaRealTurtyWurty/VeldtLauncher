package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import dev.turtywurty.veldtlauncher.instance.StoredInstanceMetadata;
import dev.turtywurty.veldtlauncher.instance.StoredModpackInstanceMetadata;
import dev.turtywurty.veldtlauncher.instance.StoredServerInstanceMetadata;
import dev.turtywurty.veldtlauncher.instance.StoredVanillaInstanceMetadata;
import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteRegistry;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.scene.control.Tooltip;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

public record StoredInstanceLibraryItemContent(StoredInstanceMetadata instance) implements LibraryItemContent {
    private static final DateTimeFormatter LAST_PLAYED_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public StoredInstanceLibraryItemContent(StoredInstanceMetadata instance) {
        this.instance = Objects.requireNonNull(instance, "instance");
    }

    @Override
    public String getName() {
        String name = this.instance.name();
        if (name != null && !name.isBlank())
            return name;

        return switch (this.instance) {
            case StoredVanillaInstanceMetadata vanilla -> fallbackVanillaName(vanilla.minecraftVersion());
            case StoredModpackInstanceMetadata modpack ->
                    fallbackModpackName(modpack.minecraftVersion(), modpack.modLoader() == null ? null : modpack.modLoader().getDisplayName());
            case StoredServerInstanceMetadata server -> fallbackServerName(server.address());
        };
    }

    @Override
    public String getIconPath() {
        return this.instance.iconPath();
    }

    @Override
    public String getDescription() {
        return switch (this.instance) {
            case StoredVanillaInstanceMetadata vanilla -> describeVanilla(vanilla.minecraftVersion());
            case StoredModpackInstanceMetadata modpack -> describeModdedInstance(
                    modpack.modLoader() == null ? null : modpack.modLoader().getDisplayName(),
                    modpack.minecraftVersion()
            );
            case StoredServerInstanceMetadata server -> describeServer(server.address(), server.minecraftVersion());
        };
    }

    @Override
    public Consumer<Navigator> getOnClickAction() {
        return navigator -> {
            if (RouteRegistry.INSTANCE.hasRoute(RouteId.VIEW_INSTANCE))
                navigator.navigateTo(RouteId.VIEW_INSTANCE);
        };
    }

    @Override
    public void onPlay(Navigator navigator, DashboardShell shell) {
        shell.showInstancePlayOverlay(this.instance);
    }

    @Override
    public Tooltip getTooltip() {
        StringBuilder builder = new StringBuilder(getDescription());

        if (this.instance instanceof StoredServerInstanceMetadata server && !server.supportedInstanceIds().isEmpty()) {
            builder.append(System.lineSeparator())
                    .append("Supports: ")
                    .append(String.join(", ", server.supportedInstanceIds()));
        }

        Path gameDirectory = this.instance.gameDirectory();
        if (gameDirectory != null) {
            builder.append(System.lineSeparator())
                    .append(gameDirectory);
        }

        Instant lastPlayedAt = this.instance.lastPlayedAt();
        if (lastPlayedAt != null) {
            builder.append(System.lineSeparator())
                    .append("Last played ")
                    .append(LAST_PLAYED_FORMATTER.format(lastPlayedAt));
        }

        return new Tooltip(builder.toString());
    }

    @Override
    public String getSearchableText() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append(' ')
                .append(getDescription()).append(' ')
                .append(this.instance.id());

        switch (this.instance) {
            case StoredVanillaInstanceMetadata vanilla -> appendIfPresent(builder, vanilla.minecraftVersion());
            case StoredModpackInstanceMetadata modpack -> {
                appendIfPresent(builder, modpack.minecraftVersion());
                if (modpack.modLoader() != null) {
                    appendIfPresent(builder, modpack.modLoader().name());
                    appendIfPresent(builder, modpack.modLoader().getDisplayName());
                }
            }
            case StoredServerInstanceMetadata server -> {
                appendIfPresent(builder, server.address());
                appendIfPresent(builder, server.minecraftVersion());
                for (String supportedInstanceId : server.supportedInstanceIds()) {
                    appendIfPresent(builder, supportedInstanceId);
                }
            }
        }

        Path gameDirectory = this.instance.gameDirectory();
        if (gameDirectory != null) {
            builder.append(' ').append(gameDirectory);
        }

        return builder.toString();
    }

    private String describeVanilla(String version) {
        return version == null || version.isBlank() ? "Vanilla instance" : "Vanilla " + version;
    }

    private String describeServer(String address, String version) {
        if (address != null && !address.isBlank() && version != null && !version.isBlank())
            return address + " • " + version;

        if (address != null && !address.isBlank())
            return address;

        return version == null || version.isBlank() ? "Server instance" : "Server " + version;
    }

    private String describeModdedInstance(String loader, String version) {
        String effectiveLoader = loader == null || loader.isBlank() ? "Modpack" : loader;
        return version == null || version.isBlank() ? effectiveLoader : effectiveLoader + " " + version;
    }

    private String fallbackVanillaName(String version) {
        return version == null || version.isBlank() ? this.instance.id() : "Minecraft " + version;
    }

    private String fallbackModpackName(String version, String loader) {
        if (loader != null && !loader.isBlank() && version != null && !version.isBlank())
            return loader + " " + version;

        if (version != null && !version.isBlank())
            return "Modpack " + version;

        return this.instance.id();
    }

    private String fallbackServerName(String address) {
        return address == null || address.isBlank() ? this.instance.id() : address;
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank())
            builder.append(' ').append(value);
    }
}
