package dev.turtywurty.veldtlauncher.minecraft.java.classpath;

import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Library;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Rule;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Artifact;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.LibraryDownloads;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultClasspathBuilder implements ClasspathBuilder {
    @Override
    public String buildClasspath(VersionMetadata metadata, InstallResult installResult) {
        if (metadata == null || installResult == null)
            return "";

        Set<String> entries = new LinkedHashSet<>();
        Path librariesDirectory = installResult.librariesDirectory();
        if (librariesDirectory != null && metadata.libraries() != null) {
            for (Library library : metadata.libraries()) {
                if (!shouldInclude(library))
                    continue;

                Path libraryPath = resolveLibraryPath(library, librariesDirectory);
                if (libraryPath != null && Files.isRegularFile(libraryPath))
                    entries.add(libraryPath.toString());
            }
        }

        Path clientJar = installResult.clientJar();
        if (clientJar != null && Files.isRegularFile(clientJar))
            entries.add(clientJar.toString());

        return String.join(File.pathSeparator, entries);
    }

    private boolean shouldInclude(Library library) {
        if (library == null)
            return false;

        List<Rule> rules = library.rules();
        if (rules == null)
            return true;

        for (Rule rule : rules) {
            if (!rule.appliesToCurrentEnvironment())
                return false;
        }

        return true;
    }

    private Path resolveLibraryPath(Library library, Path librariesDirectory) {
        Artifact artifact = resolveLibraryArtifact(library);
        if (artifact == null)
            return null;

        String artifactPath = artifact.path();
        if (artifactPath == null || artifactPath.isBlank())
            artifactPath = deriveArtifactPathFromUri(artifact.uri());

        if ((artifactPath == null || artifactPath.isBlank()) && library.name() != null && library.url() != null)
            artifactPath = buildArtifactPath(library.name(), null);

        if (artifactPath == null || artifactPath.isBlank())
            return null;

        return librariesDirectory.resolve(artifactPath);
    }

    private Artifact resolveLibraryArtifact(Library library) {
        LibraryDownloads downloads = library.downloads();
        if (downloads == null)
            return createFallbackArtifact(library, null);

        Artifact artifact = downloads.artifact();
        if (artifact != null)
            return artifact;

        return createFallbackArtifact(library, null);
    }

    private Artifact createFallbackArtifact(Library library, String classifier) {
        URI libraryUrl = library.url();
        if (libraryUrl == null || library.name() == null || library.name().isBlank())
            return null;

        String baseUrl = libraryUrl.toString();
        String artifactPath = buildArtifactPath(library.name(), classifier);
        if (artifactPath == null)
            return null;

        URI uri = URI.create((baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + artifactPath);
        return new Artifact(artifactPath, null, 0L, uri);
    }

    private String deriveArtifactPathFromUri(URI uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isBlank())
            return null;

        String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String buildArtifactPath(String coordinate, String classifier) {
        String[] parts = coordinate.split(":");
        if (parts.length < 3 || parts.length > 4)
            return null;

        String groupPath = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String declaredClassifier = parts.length == 4 ? parts[3] : null;
        String effectiveClassifier = classifier != null && !classifier.isBlank() ? classifier : declaredClassifier;

        String fileName = artifactId + "-" + version;
        if (effectiveClassifier != null && !effectiveClassifier.isBlank())
            fileName += "-" + effectiveClassifier;

        fileName += ".jar";
        return groupPath + "/" + artifactId + "/" + version + "/" + fileName;
    }
}
