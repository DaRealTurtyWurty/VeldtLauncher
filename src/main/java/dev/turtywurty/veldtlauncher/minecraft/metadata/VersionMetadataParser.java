package dev.turtywurty.veldtlauncher.minecraft.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.*;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.*;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Artifact;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Download;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Downloads;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.LibraryDownloads;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.Logging;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingConfig;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingFile;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VersionMetadataParser {
    private VersionMetadataParser() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static VersionMetadata parse(JsonObject json) {
        if (json == null)
            throw new IllegalArgumentException("Version metadata JSON cannot be null");

        return new VersionMetadata(
                parseArguments(JsonUtil.getObject(json, "arguments")),
                parseAssetIndex(JsonUtil.getObject(json, "assetIndex")),
                JsonUtil.getString(json, "assets"),
                JsonUtil.getString(json, "complianceLevel"),
                parseDownloads(JsonUtil.getObject(json, "downloads")),
                JsonUtil.getString(json, "id"),
                parseJavaVersion(JsonUtil.getObject(json, "javaVersion")),
                parseLibraries(JsonUtil.getArray(json, "libraries")),
                parseLogging(JsonUtil.getObject(json, "logging")),
                JsonUtil.getString(json, "mainClass"),
                JsonUtil.getInt(json, "minimumLauncherVersion"),
                JsonUtil.getInstant(json, "releaseTime"),
                JsonUtil.getInstant(json, "time"),
                JsonUtil.getString(json, "type")
        );
    }

    private static Arguments parseArguments(JsonObject json) {
        if (json == null)
            return null;

        return new Arguments(
                parseDefaultUserJvmArguments(JsonUtil.getObject(json, "defaultUserJvm")),
                parseArgumentList(JsonUtil.getArray(json, "game")),
                parseArgumentList(JsonUtil.getArray(json, "jvm"))
        );
    }

    private static DefaultUserJvmArguments parseDefaultUserJvmArguments(JsonObject json) {
        if (json == null)
            return null;

        return new DefaultUserJvmArguments(
                parseStringValues(JsonUtil.getArray(json, "value")),
                parseRules(JsonUtil.getArray(json, "rules"))
        );
    }

    private static List<Argument> parseArgumentList(JsonArray json) {
        if (json == null || json.isEmpty())
            return List.of();

        List<Argument> arguments = new ArrayList<>();
        for (JsonElement element : json) {
            if (element == null || element.isJsonNull())
                continue;

            if (element.isJsonPrimitive()) {
                arguments.add(new StringArgument(element.getAsString()));
                continue;
            }

            if (!element.isJsonObject())
                continue;

            JsonObject argumentObject = element.getAsJsonObject();
            arguments.add(new RuleArgument(
                    parseRuleArgumentValues(argumentObject.get("value")),
                    parseArgumentRules(JsonUtil.getArray(argumentObject, "rules"))
            ));
        }

        return List.copyOf(arguments);
    }

    private static String[] parseRuleArgumentValues(JsonElement element) {
        if (element == null || element.isJsonNull())
            return new String[0];

        if (element.isJsonPrimitive())
            return new String[]{element.getAsString()};

        if (!element.isJsonArray())
            return new String[0];

        List<String> values = new ArrayList<>();
        for (JsonElement arrayElement : element.getAsJsonArray()) {
            if (arrayElement == null || arrayElement.isJsonNull())
                continue;

            values.add(arrayElement.getAsString());
        }

        return values.toArray(String[]::new);
    }

    private static List<ArgumentRule> parseArgumentRules(JsonArray json) {
        if (json == null || json.isEmpty())
            return List.of();

        List<ArgumentRule> rules = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject ruleObject = asObject(element);
            if (ruleObject == null)
                continue;

            rules.add(new ArgumentRule(
                    JsonUtil.getString(ruleObject, "action"),
                    parseFeatures(JsonUtil.getObject(ruleObject, "features"))
            ));
        }

        return List.copyOf(rules);
    }

    private static Map<String, Boolean> parseFeatures(JsonObject json) {
        if (json == null || json.entrySet().isEmpty())
            return Map.of();

        Map<String, Boolean> features = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull())
                continue;

            try {
                features.put(entry.getKey(), value.getAsBoolean());
            } catch (RuntimeException ignored) {
            }
        }

        return Map.copyOf(features);
    }

    private static AssetIndex parseAssetIndex(JsonObject json) {
        if (json == null)
            return null;

        return new AssetIndex(
                JsonUtil.getString(json, "id"),
                JsonUtil.getString(json, "sha1"),
                JsonUtil.getLong(json, "totalSize"),
                parseUri(JsonUtil.getString(json, "url")),
                JsonUtil.getLong(json, "size")
        );
    }

    private static Downloads parseDownloads(JsonObject json) {
        if (json == null)
            return null;

        return new Downloads(
                parseDownload(JsonUtil.getObject(json, "client")),
                parseDownload(JsonUtil.getObject(json, "client_mappings")),
                parseDownload(JsonUtil.getObject(json, "server")),
                parseDownload(JsonUtil.getObject(json, "server_mappings"))
        );
    }

    private static Download parseDownload(JsonObject json) {
        if (json == null)
            return null;

        return new Download(
                JsonUtil.getString(json, "sha1"),
                JsonUtil.getLong(json, "size"),
                parseUri(JsonUtil.getString(json, "url"))
        );
    }

    private static JavaVersion parseJavaVersion(JsonObject json) {
        if (json == null)
            return null;

        return new JavaVersion(
                JsonUtil.getString(json, "component"),
                JsonUtil.getInt(json, "majorVersion")
        );
    }

    private static List<Library> parseLibraries(JsonArray json) {
        if (json == null || json.isEmpty())
            return List.of();

        List<Library> libraries = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject libraryObject = asObject(element);
            if (libraryObject == null)
                continue;

            libraries.add(new Library(
                    parseLibraryDownloads(JsonUtil.getObject(libraryObject, "downloads")),
                    JsonUtil.getString(libraryObject, "name"),
                    parseUri(JsonUtil.getString(libraryObject, "url")),
                    parseRules(JsonUtil.getArray(libraryObject, "rules")),
                    parseNative(JsonUtil.getObject(libraryObject, "natives")),
                    parseExtract(JsonUtil.getObject(libraryObject, "extract"))
            ));
        }

        return List.copyOf(libraries);
    }

    private static LibraryDownloads parseLibraryDownloads(JsonObject json) {
        if (json == null)
            return null;

        return new LibraryDownloads(
                parseArtifact(JsonUtil.getObject(json, "artifact")),
                parseArtifactMap(JsonUtil.getObject(json, "classifiers"))
        );
    }

    private static Artifact parseArtifact(JsonObject json) {
        if (json == null)
            return null;

        return new Artifact(
                JsonUtil.getString(json, "path"),
                JsonUtil.getString(json, "sha1"),
                JsonUtil.getLong(json, "size"),
                parseUri(JsonUtil.getString(json, "url"))
        );
    }

    private static Map<String, Artifact> parseArtifactMap(JsonObject json) {
        if (json == null || json.entrySet().isEmpty())
            return Map.of();

        Map<String, Artifact> artifacts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonObject artifactObject = asObject(entry.getValue());
            if (artifactObject == null)
                continue;

            artifacts.put(entry.getKey(), parseArtifact(artifactObject));
        }

        return Map.copyOf(artifacts);
    }

    private static Native parseNative(JsonObject json) {
        if (json == null)
            return null;

        return new Native(
                JsonUtil.getString(json, "linux"),
                JsonUtil.getString(json, "macos"),
                JsonUtil.getString(json, "windows"),
                JsonUtil.getString(json, "osx")
        );
    }

    private static Extract parseExtract(JsonObject json) {
        if (json == null)
            return null;

        return new Extract(parseStringValues(JsonUtil.getArray(json, "exclude")));
    }

    private static List<Rule> parseRules(JsonArray json) {
        if (json == null || json.isEmpty())
            return List.of();

        List<Rule> rules = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject ruleObject = asObject(element);
            if (ruleObject == null)
                continue;

            rules.add(new Rule(
                    JsonUtil.getString(ruleObject, "action"),
                    parseOs(JsonUtil.getObject(ruleObject, "os"))
            ));
        }

        return List.copyOf(rules);
    }

    private static Os parseOs(JsonObject json) {
        if (json == null)
            return null;

        return new Os(
                JsonUtil.getString(json, "name"),
                JsonUtil.getString(json, "arch"),
                parseVersionRange(json.get("versionRange"))
        );
    }

    private static VersionRange parseVersionRange(JsonElement element) {
        if (element == null || element.isJsonNull())
            return null;

        if (element.isJsonPrimitive()) {
            String version = element.getAsString();
            return new VersionRange(version, version);
        }

        if (!element.isJsonObject())
            return null;

        JsonObject json = element.getAsJsonObject();
        return new VersionRange(
                JsonUtil.getString(json, "min"),
                JsonUtil.getString(json, "max")
        );
    }

    private static Logging parseLogging(JsonObject json) {
        if (json == null)
            return null;

        return new Logging(parseLoggingConfig(JsonUtil.getObject(json, "client")));
    }

    private static LoggingConfig parseLoggingConfig(JsonObject json) {
        if (json == null)
            return null;

        return new LoggingConfig(
                JsonUtil.getString(json, "argument"),
                parseLoggingFile(JsonUtil.getObject(json, "file")),
                JsonUtil.getString(json, "type")
        );
    }

    private static LoggingFile parseLoggingFile(JsonObject json) {
        if (json == null)
            return null;

        return new LoggingFile(
                JsonUtil.getString(json, "id"),
                JsonUtil.getString(json, "sha1"),
                JsonUtil.getLong(json, "size"),
                parseUri(JsonUtil.getString(json, "url"))
        );
    }

    private static List<String> parseStringValues(JsonArray json) {
        if (json == null || json.isEmpty())
            return List.of();

        List<String> values = new ArrayList<>();
        for (JsonElement element : json) {
            if (element == null || element.isJsonNull())
                continue;

            values.add(element.getAsString());
        }

        return List.copyOf(values);
    }

    private static JsonObject asObject(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonObject())
            return null;

        return element.getAsJsonObject();
    }

    private static URI parseUri(String value) {
        if (value == null || value.isBlank())
            return null;

        return URI.create(value);
    }
}
