package dev.turtywurty.veldtlauncher.minecraft.launch.args;

import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftProfile;
import dev.turtywurty.veldtlauncher.auth.session.MinecraftSession;
import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.java.JDK;
import dev.turtywurty.veldtlauncher.minecraft.java.classpath.ClasspathBuilder;
import dev.turtywurty.veldtlauncher.minecraft.java.classpath.DefaultClasspathBuilder;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.AssetIndex;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.Argument;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.ArgumentRule;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.Arguments;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.DefaultUserJvmArguments;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.RuleArgument;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.Logging;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingConfig;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultLaunchArgumentBuilder implements LaunchArgumentBuilder {
    private static final String LAUNCHER_NAME = "VeldtLauncher";
    private static final String LAUNCHER_VERSION = "1.0.0";
    private static final String DEFAULT_RESOLUTION_WIDTH = "854";
    private static final String DEFAULT_RESOLUTION_HEIGHT = "480";

    private final ClasspathBuilder classpathBuilder;

    public DefaultLaunchArgumentBuilder(ClasspathBuilder classpathBuilder) {
        this.classpathBuilder = classpathBuilder;
    }

    public DefaultLaunchArgumentBuilder() {
        this(new DefaultClasspathBuilder());
    }

    @Override
    public LaunchArguments build(
            VersionMetadata metadata,
            InstallResult installResult,
            MinecraftSession session,
            JDK jdk
    ) {
        if (metadata == null)
            throw new IllegalArgumentException("Metadata cannot be null");

        if (installResult == null)
            throw new IllegalArgumentException("Install result cannot be null");

        if (session == null)
            throw new IllegalArgumentException("Session cannot be null");

        if (jdk == null)
            throw new IllegalArgumentException("JDK cannot be null");

        Arguments arguments = metadata.arguments();
        Map<String, String> placeholders = createPlaceholderValues(metadata, installResult, session, jdk);
        Map<String, Boolean> features = createFeatureValues();

        List<String> jvmArguments = new ArrayList<>();
        if (arguments != null) {
            addDefaultUserJvmArguments(arguments.defaultUserJvm(), jvmArguments, placeholders);
            addArguments(arguments.jvm(), jvmArguments, placeholders, features);
        }

        addLoggingArgument(metadata.logging(), installResult, jvmArguments, placeholders);

        List<String> gameArguments = new ArrayList<>();
        if (arguments != null)
            addArguments(arguments.game(), gameArguments, placeholders, features);

        return new LaunchArguments(List.copyOf(jvmArguments), List.copyOf(gameArguments));
    }

    private void addDefaultUserJvmArguments(
            DefaultUserJvmArguments defaultUserJvm,
            List<String> destination,
            Map<String, String> placeholders
    ) {
        if (defaultUserJvm == null || defaultUserJvm.value() == null)
            return;

        if (defaultUserJvm.rules() != null) {
            for (var rule : defaultUserJvm.rules()) {
                if (!rule.appliesToCurrentEnvironment())
                    return;
            }
        }

        for (String value : defaultUserJvm.value()) {
            if (value == null || value.isBlank())
                continue;

            String resolved = replacePlaceholders(value, placeholders);
            if (!containsUnresolvedPlaceholder(resolved))
                destination.add(resolved);
        }
    }

    private void addArguments(
            List<Argument> arguments,
            List<String> destination,
            Map<String, String> placeholders,
            Map<String, Boolean> features
    ) {
        if (arguments == null)
            return;

        for (Argument argument : arguments) {
            if (argument == null || !shouldInclude(argument, features))
                continue;

            String[] values = argument instanceof RuleArgument ruleArgument
                    ? ruleArgument.value()
                    : new String[]{argument.getValue()};
            if (values == null || values.length == 0)
                continue;

            List<String> resolvedValues = new ArrayList<>(values.length);
            boolean unresolved = false;
            for (String value : values) {
                if (value == null || value.isBlank())
                    continue;

                String resolved = replacePlaceholders(value, placeholders);
                if (containsUnresolvedPlaceholder(resolved)) {
                    unresolved = true;
                    break;
                }

                resolvedValues.add(resolved);
            }

            if (!unresolved)
                destination.addAll(resolvedValues);
        }
    }

    private boolean shouldInclude(Argument argument, Map<String, Boolean> features) {
        List<ArgumentRule> rules = argument.getRules();
        if (rules == null || rules.isEmpty())
            return true;

        boolean allowed = false;
        for (ArgumentRule rule : rules) {
            if (!rule.appliesToCurrentEnvironment(features))
                continue;

            allowed = "allow".equals(rule.action());
            if ("disallow".equals(rule.action()))
                allowed = false;
        }

        return allowed;
    }

    private void addLoggingArgument(
            Logging logging,
            InstallResult installResult,
            List<String> destination,
            Map<String, String> placeholders
    ) {
        if (logging == null || logging.client() == null)
            return;

        LoggingConfig clientLogging = logging.client();
        String argument = clientLogging.argument();
        if (argument == null || argument.isBlank())
            return;

        Map<String, String> loggingPlaceholders = new LinkedHashMap<>(placeholders);
        Path loggingPath = resolveLoggingPath(clientLogging.file(), installResult);
        loggingPlaceholders.put("path", pathString(loggingPath));

        String resolved = replacePlaceholders(argument, loggingPlaceholders);
        if (!containsUnresolvedPlaceholder(resolved))
            destination.add(resolved);
    }

    private Path resolveLoggingPath(LoggingFile loggingFile, InstallResult installResult) {
        if (loggingFile == null || loggingFile.id() == null || loggingFile.id().isBlank())
            return null;

        Path assetsDirectory = installResult.assetsDirectory();
        if (assetsDirectory == null)
            return null;

        return assetsDirectory.resolve("log_configs").resolve(loggingFile.id());
    }

    private Map<String, String> createPlaceholderValues(
            VersionMetadata metadata,
            InstallResult installResult,
            MinecraftSession session,
            JDK jdk
    ) {
        MinecraftProfile profile = session.profile();
        Path gameDirectory = resolveMinecraftDirectory(installResult.gameDirectory());
        AssetIndex assetIndex = metadata.assetIndex();
        String classpath = classpathBuilder.buildClasspath(metadata, installResult);

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("auth_player_name", profile == null ? "" : nullToEmpty(profile.name()));
        placeholders.put("version_name", nullToEmpty(metadata.id()));
        placeholders.put("game_directory", pathString(gameDirectory));
        placeholders.put("assets_root", pathString(installResult.assetsDirectory()));
        placeholders.put("assets_index_name", assetIndex != null && assetIndex.id() != null && !assetIndex.id().isBlank()
                ? assetIndex.id()
                : nullToEmpty(metadata.assets()));
        placeholders.put("auth_uuid", profile == null ? "" : nullToEmpty(profile.id()));
        placeholders.put("auth_access_token", nullToEmpty(session.accessToken()));
        placeholders.put("auth_session", nullToEmpty(session.accessToken()));
        placeholders.put("clientid", "");
        placeholders.put("auth_xuid", "");
        placeholders.put("user_type", "msa");
        placeholders.put("version_type", nullToEmpty(metadata.type()));
        placeholders.put("natives_directory", pathString(installResult.nativesDirectory()));
        placeholders.put("launcher_name", LAUNCHER_NAME);
        placeholders.put("launcher_version", LAUNCHER_VERSION);
        placeholders.put("classpath", classpath);
        placeholders.put("classpath_separator", File.pathSeparator);
        placeholders.put("library_directory", pathString(installResult.librariesDirectory()));
        placeholders.put("user_properties", "{}");
        placeholders.put("resolution_width", DEFAULT_RESOLUTION_WIDTH);
        placeholders.put("resolution_height", DEFAULT_RESOLUTION_HEIGHT);
        placeholders.put("quickPlayPath", "");
        placeholders.put("game_assets", pathString(installResult.assetsDirectory() == null
                ? null
                : installResult.assetsDirectory().resolve("virtual").resolve("legacy")));
        placeholders.put("java_directory", pathString(jdk.path()));
        return placeholders;
    }

    private Map<String, Boolean> createFeatureValues() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("is_demo_user", false);
        features.put("has_custom_resolution", false);
        features.put("has_quick_plays_support", false);
        features.put("is_quick_play_singleplayer", false);
        features.put("is_quick_play_multiplayer", false);
        features.put("is_quick_play_realms", false);
        return Map.copyOf(features);
    }

    private Path resolveMinecraftDirectory(Path launcherDirectory) {
        if (launcherDirectory == null)
            return null;

        return launcherDirectory.resolve(".minecraft");
    }

    private String replacePlaceholders(String value, Map<String, String> placeholders) {
        String resolved = value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("${" + entry.getKey() + "}", nullToEmpty(entry.getValue()));
        }

        return resolved;
    }

    private boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("${") && value.contains("}");
    }

    private String pathString(Path path) {
        return path == null ? "" : path.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
