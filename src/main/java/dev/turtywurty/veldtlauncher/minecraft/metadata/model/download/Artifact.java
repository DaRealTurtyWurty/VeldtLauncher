package dev.turtywurty.veldtlauncher.minecraft.metadata.model.download;

import com.google.gson.annotations.SerializedName;

import java.net.URI;

public record Artifact(
        String path,
        String sha1,
        long size,
        @SerializedName(value = "url", alternate = {"uri"})
        URI uri
) {
}
