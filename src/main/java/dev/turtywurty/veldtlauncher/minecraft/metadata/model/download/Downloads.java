package dev.turtywurty.veldtlauncher.minecraft.metadata.model.download;

import com.google.gson.annotations.SerializedName;

public record Downloads(
        Download client,
        @SerializedName("client_mappings")
        Download clientMappings,
        Download server,
        @SerializedName("server_mappings")
        Download serverMappings
) {
}
