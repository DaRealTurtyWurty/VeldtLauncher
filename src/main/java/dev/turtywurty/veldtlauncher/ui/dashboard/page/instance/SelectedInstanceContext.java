package dev.turtywurty.veldtlauncher.ui.dashboard.page.instance;

import dev.turtywurty.veldtlauncher.instance.JsonInstanceStore;
import dev.turtywurty.veldtlauncher.instance.StoredInstanceMetadata;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class SelectedInstanceContext {
    private static final AtomicReference<String> SELECTED_INSTANCE_ID = new AtomicReference<>();

    private SelectedInstanceContext() {
    }

    public static void select(StoredInstanceMetadata instance) {
        Objects.requireNonNull(instance, "instance");
        select(instance.id());
    }

    public static void select(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            clear();
            return;
        }

        SELECTED_INSTANCE_ID.set(instanceId);
    }

    public static Optional<String> getSelectedInstanceId() {
        return Optional.ofNullable(SELECTED_INSTANCE_ID.get())
                .filter(instanceId -> !instanceId.isBlank());
    }

    public static Optional<StoredInstanceMetadata> loadSelected() {
        return getSelectedInstanceId().flatMap(JsonInstanceStore.INSTANCE::load);
    }

    public static void clear() {
        SELECTED_INSTANCE_ID.set(null);
    }
}
